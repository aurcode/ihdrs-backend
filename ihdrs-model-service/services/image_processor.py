# services/image_processor.py

import cv2
import numpy as np
from PIL import Image
import io
import logging
import time
from typing import Optional, Tuple, List

logger = logging.getLogger(__name__)

class ImageProcessor:
    """图像处理服务类，将用户上传的手写图片转换成模型可识别的标准输入(28×28×1)"""

    def __init__(self, target_size: Tuple[int, int] = (28, 28)):
        self.target_size = target_size

    def preprocess_for_recognition(self, image_bytes: bytes) -> Optional[np.ndarray]:
        """
        为识别预处理图像数据

        Args:
            image_bytes: 原始图像字节数据

        Returns:
            预处理后的图像数组，形状为 (28, 28) 或 None（如果处理失败）
        """
        try:
            # 1. 字节数据转PIL图像
            image = Image.open(io.BytesIO(image_bytes))

            # 2. 转换为RGB（如果是RGBA或其他格式）
            if image.mode != 'RGB':
                if image.mode == 'RGBA':
                    # 创建白色背景
                    background = Image.new('RGB', image.size, (255, 255, 255))
                    background.paste(image, mask=image.split()[-1])
                    image = background
                else:
                    image = image.convert('RGB')

            # 3. 转换为numpy数组
            image_array = np.array(image)

            # 4. 转换为灰度图
            if len(image_array.shape) == 3:
                image_gray = cv2.cvtColor(image_array, cv2.COLOR_RGB2GRAY)
            else:
                image_gray = image_array

            # 5. 预处理步骤
            processed_image = self._preprocess_pipeline(image_gray)

            return processed_image

        except Exception as e:
            logger.error(f"图像预处理失败: {e}")
            return None

    def _preprocess_pipeline(self, image: np.ndarray) -> np.ndarray:
        """
        图像预处理管道

        Args:
            image: 灰度图像数组

        Returns:
            预处理后的图像数组
        """
        # 1. 去噪
        denoised = cv2.fastNlMeansDenoising(image)

        # 2. 二值化
        _, binary = cv2.threshold(denoised, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)

        # 3. 查找轮廓并提取感兴趣区域
        roi = self._extract_roi(binary)

        # 4. 调整大小到28x28
        resized = cv2.resize(roi, self.target_size, interpolation=cv2.INTER_AREA)

        # 5. 归一化到[0,1]
        normalized = resized.astype(np.float32) / 255.0

        # 6. 数据增强
        # enhanced = self._apply_augmentation(normalized)

        return normalized

    def _extract_roi(self, binary_image: np.ndarray) -> np.ndarray:
        """
        提取感兴趣区域（数字部分）

        Args:
            binary_image: 二值化图像

        Returns:
            提取的ROI区域
        """
        try:
            # 查找轮廓
            contours, _ = cv2.findContours(binary_image, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

            if not contours:
                # 如果没有找到轮廓，返回原图像
                return binary_image

            # 找到最大轮廓
            largest_contour = max(contours, key=cv2.contourArea)

            # 获取边界框
            x, y, w, h = cv2.boundingRect(largest_contour)

            # 添加一些边距
            margin = 5
            x = max(0, x - margin)
            y = max(0, y - margin)
            w = min(binary_image.shape[1] - x, w + 2 * margin)
            h = min(binary_image.shape[0] - y, h + 2 * margin)

            # 提取ROI
            roi = binary_image[y:y+h, x:x+w]

            # 如果ROI太小，返回原图像
            if roi.shape[0] < 10 or roi.shape[1] < 10:
                return binary_image

            return roi

        except Exception as e:
            logger.warning(f"ROI提取失败，使用原图像: {e}")
            return binary_image

    def _apply_augmentation(self, image: np.ndarray) -> np.ndarray:
        """
        应用数据增强（仅在训练时使用）

        Args:
            image: 输入图像

        Returns:
            增强后的图像
        """
        # 可以添加轻微的旋转、平移等
        # 这里暂不实现，保持原图像
        return image

    def segment_digits(self, image_bytes: bytes, debug: bool = False) -> List[np.ndarray]:
        """
        分割连续数字（增强版：抗阴影、抗边框、分离粘连、优化性能）
        
        Args:
            image_bytes: 图像字节数据
            debug: 是否启用调试模式
            
        Returns:
            分割后的数字图像列表
        """
        start_time = time.time()
        debug_info = {}
        
        try:
            # 1. 基础加载 - 优化内存使用
            image = Image.open(io.BytesIO(image_bytes))
            if image.mode != 'RGB':
                image = image.convert('RGB')
            
            # 限制图像尺寸以提高处理速度
            original_size = image.size
            image.thumbnail((800, 800), Image.Resampling.LANCZOS)
            resized_size = image.size
            
            if debug:
                debug_info['original_size'] = original_size
                debug_info['resized_size'] = resized_size
            
            image_array = np.array(image)

            # 转灰度
            if len(image_array.shape) == 3:
                gray = cv2.cvtColor(image_array, cv2.COLOR_RGB2GRAY)
            else:
                gray = image_array

            # 2. 预处理优化 - 自适应参数
            # 2.1 高斯模糊去噪 - 根据图像大小调整核大小
            h_img, w_img = gray.shape
            blur_kernel = (5, 5) if max(h_img, w_img) > 200 else (3, 3)
            blurred = cv2.GaussianBlur(gray, blur_kernel, 0)

            # 2.2 自适应阈值 - 根据图像特征动态调整参数
            # 计算图像亮度以调整阈值参数
            mean_brightness = np.mean(blurred)
            if mean_brightness > 200:  # 亮背景
                block_size = 21
                c_value = 8
            elif mean_brightness > 150:  # 中等亮度
                block_size = 19
                c_value = 5
            else:  # 暗背景
                block_size = 15
                c_value = 3

            binary = cv2.adaptiveThreshold(
                blurred, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                cv2.THRESH_BINARY_INV, block_size, c_value
            )

            if debug:
                debug_info['brightness'] = float(mean_brightness)
                debug_info['threshold_params'] = {'block_size': block_size, 'c_value': c_value}

            # 2.3 清除边缘 - 动态边框大小
            border_size = max(2, min(10, min(h_img, w_img) // 50))
            cv2.rectangle(binary, (0, 0), (w_img, h_img), (0, 0, 0), border_size * 2)

            # 2.4 智能形态学操作 - 根据笔画宽度调整
            # 计算平均笔画宽度
            stroke_width = self._estimate_stroke_width(binary)
            
            kernel_size = max(2, min(4, stroke_width // 2))
            kernel = np.ones((kernel_size, kernel_size), np.uint8)
            
            # 根据粘连程度决定腐蚀强度
            connectivity = self._analyze_connectivity(binary)
            iterations = 1 if connectivity < 0.3 else (2 if connectivity < 0.6 else 0)
            
            if iterations > 0:
                eroded = cv2.erode(binary, kernel, iterations=iterations)
            else:
                eroded = binary

            if debug:
                debug_info['stroke_width'] = stroke_width
                debug_info['connectivity'] = float(connectivity)
                debug_info['morphology'] = {'kernel_size': kernel_size, 'iterations': iterations}

            # 3. 查找轮廓 - 使用更精确的方法
            contours, hierarchy = cv2.findContours(eroded, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

            if not contours:
                logger.warning("未检测到任何轮廓")
                return []

            if debug:
                debug_info['total_contours'] = len(contours)

            # 4. 智能轮廓筛选与提取
            digits = []
            bounding_boxes = []

            image_area = h_img * w_img

            # 分析轮廓层级关系
            valid_contours = []
            for i, contour in enumerate(contours):
                # 跳过父轮廓（可能是整个图像）
                if hierarchy[0][i][3] != -1:
                    continue
                    
                x, y, w, h = cv2.boundingRect(contour)
                area = w * h

                # 多层过滤策略
                if not self._is_valid_digit_contour(area, w, h, image_area, contour):
                    continue

                valid_contours.append((x, y, w, h, contour))

            if debug:
                debug_info['valid_contours'] = len(valid_contours)

            # 合并重叠的轮廓（处理断裂的数字）
            merged_contours = self._merge_overlapping_contours(valid_contours)

            if debug:
                debug_info['merged_contours'] = len(merged_contours)

            # 按 x 坐标排序 (从左到右)
            merged_contours.sort(key=lambda b: b[0])

            for x, y, w, h, contour in merged_contours:
                # 智能填充和边缘处理
                digit_roi = self._extract_digit_roi(binary, x, y, w, h, h_img, w_img)
                
                if digit_roi is not None:
                    # 使用优化的缩放方法
                    processed_digit = self._resize_with_pad(digit_roi)
                    digits.append(processed_digit)

            processing_time = time.time() - start_time
            
            if debug:
                debug_info['processing_time'] = round(processing_time, 3)
                debug_info['final_digits'] = len(digits)
                logger.info(f"分割调试信息: {debug_info}")

            logger.info(f"成功分割出 {len(digits)} 个数字 (耗时: {processing_time:.3f}s)")
            return digits

        except Exception as e:
            logger.error(f"数字分割失败: {e}", exc_info=True)
            return []

    def _resize_with_pad(self, image: np.ndarray) -> np.ndarray:
        """
        将图像调整为目标尺寸，模拟 MNIST 数据集特征：
        1. 保持长宽比缩放
        2. 适当加粗笔画
        3. 使用重心(Center of Mass)居中，而不是几何中心
        """
        # --- 步骤 1: 适当加粗 (解决笔画太细的问题) ---
        # 如果原图很大，先做一次轻微膨胀，防止缩放后断裂
        # 这里的 kernel 大小取决于原图的分辨率，如果原图是 Canvas 导出的高也就是几百像素，(2,2) 或 (3,3) 比较合适
        kernel = np.ones((3, 3), np.uint8)
        image = cv2.dilate(image, kernel, iterations=1)

        h, w = image.shape[:2]
        target_h, target_w = self.target_size # (28, 28)

        # MNIST 的数字通常在一个 20x20 的框内，放在 28x28 的画布中心
        # 留出 4 像素的边距 (padding)
        inner_size = 20

        # --- 步骤 2: 保持比例缩放 ---
        scale = min(inner_size / h, inner_size / w)
        new_w = int(w * scale)
        new_h = int(h * scale)

        # 缩放图像
        resized = cv2.resize(image, (new_w, new_h), interpolation=cv2.INTER_AREA)

        # 创建黑色画布 (28x28)
        canvas = np.zeros((target_h, target_w), dtype=np.uint8)

        # 先把缩放后的图贴到几何中心
        y_offset = (target_h - new_h) // 2
        x_offset = (target_w - new_w) // 2
        canvas[y_offset:y_offset+new_h, x_offset:x_offset+new_w] = resized

        # --- 步骤 3: 重心校正 (Center of Mass) ---
        # 这是大幅提升准确率的关键
        final_image = self._center_image(canvas)

        # --- 步骤 4: 归一化 ---
        normalized = final_image.astype(np.float32) / 255.0

        return normalized

    def _center_image(self, image: np.ndarray) -> np.ndarray:
        """
        根据像素重心将图像居中
        """
        (h, w) = image.shape

        # 计算图像矩
        M = cv2.moments(image)

        # 防止除以零（全黑图像）
        if M["m00"] == 0:
            return image

        # 计算重心坐标
        cX = M["m10"] / M["m00"]
        cY = M["m01"] / M["m00"]

        # 计算偏离中心的距离 (图像中心是 14, 14)
        shift_x = np.round(w / 2.0 - cX).astype(int)
        shift_y = np.round(h / 2.0 - cY).astype(int)

        # 构建平移矩阵
        M_affine = np.float32([[1, 0, shift_x], [0, 1, shift_y]])

        # 执行平移
        shifted = cv2.warpAffine(image, M_affine, (w, h))

        return shifted

    def _estimate_stroke_width(self, binary_image: np.ndarray) -> int:
        """
        估计笔画宽度 - 用于优化形态学操作
        """
        try:
            # 使用距离变换估计笔画宽度
            dist_transform = cv2.distanceTransform(binary_image, cv2.DIST_L2, 3)
            
            # 找到最大距离（笔画中心到边缘的距离）
            max_dist = np.max(dist_transform)
            
            # 笔画宽度大约是最大距离的两倍
            stroke_width = int(max_dist * 2)
            
            # 限制在合理范围内
            return max(2, min(10, stroke_width))
            
        except Exception as e:
            logger.warning(f"笔画宽度估计失败: {e}")
            return 3  # 默认值

    def _analyze_connectivity(self, binary_image: np.ndarray) -> float:
        """
        分析数字之间的粘连程度
        """
        try:
            # 计算水平投影
            horizontal_proj = np.sum(binary_image, axis=1)
            
            # 找到投影的谷值（数字之间的间隙）
            valleys = []
            for i in range(1, len(horizontal_proj) - 1):
                if (horizontal_proj[i] < horizontal_proj[i-1] and 
                    horizontal_proj[i] < horizontal_proj[i+1]):
                    valleys.append(i)
            
            # 计算粘连程度 - 谷值越少，粘连越严重
            if len(valleys) == 0:
                return 1.0  # 完全粘连
            
            # 计算平均谷值深度
            total_proj = np.sum(horizontal_proj)
            avg_valley_depth = np.mean([horizontal_proj[v] for v in valleys]) if valleys else 0
            
            connectivity = avg_valley_depth / (total_proj / len(horizontal_proj)) if total_proj > 0 else 0
            
            return min(1.0, connectivity)
            
        except Exception as e:
            logger.warning(f"粘连分析失败: {e}")
            return 0.5  # 默认值

    def _is_valid_digit_contour(self, area: int, width: int, height: int, image_area: int, contour: np.ndarray) -> bool:
        """
        智能判断轮廓是否为有效数字
        """
        try:
            # 基本尺寸过滤
            if area < 30 or width < 4 or height < 8:  # 太小
                return False
            
            if area > image_area * 0.4:  # 太大（可能是背景）
                return False
            
            # 长宽比过滤
            aspect_ratio = width / height if height > 0 else 0
            if aspect_ratio > 3.0 or aspect_ratio < 0.1:  # 过于细长
                return False
            
            #  solidity 过滤（轮廓面积与边界框面积的比例）
            contour_area = cv2.contourArea(contour)
            solidity = contour_area / area if area > 0 else 0
            if solidity < 0.2:  # 过于稀疏
                return False
            
            # 密度过滤（像素密度）
            pixel_density = np.sum(cv2.drawContours(np.zeros((height, width), dtype=np.uint8), 
                                                 [contour], -1, 255, -1)) / area if area > 0 else 0
            if pixel_density < 0.3:  # 像素密度太低
                return False
            
            return True
            
        except Exception as e:
            logger.warning(f"轮廓验证失败: {e}")
            return False

    def _merge_overlapping_contours(self, contours: List[tuple]) -> List[tuple]:
        """
        合并重叠的轮廓（处理断裂的数字）
        """
        if not contours:
            return []
        
        try:
            merged = []
            used = [False] * len(contours)
            
            for i, (x1, y1, w1, h1, contour1) in enumerate(contours):
                if used[i]:
                    continue
                
                # 当前轮廓
                current_x, current_y = x1, y1
                current_right = x1 + w1
                current_bottom = y1 + h1
                
                # 查找重叠的轮廓
                for j in range(i + 1, len(contours)):
                    if used[j]:
                        continue
                    
                    x2, y2, w2, h2, contour2 = contours[j]
                    
                    # 检查是否重叠
                    overlap_threshold = 0.3  # 30% 重叠
                    
                    # 计算重叠区域
                    overlap_x1 = max(current_x, x2)
                    overlap_y1 = max(current_y, y2)
                    overlap_x2 = min(current_right, x2 + w2)
                    overlap_y2 = min(current_bottom, y2 + h2)
                    
                    if overlap_x1 < overlap_x2 and overlap_y1 < overlap_y2:
                        overlap_area = (overlap_x2 - overlap_x1) * (overlap_y2 - overlap_y1)
                        area1 = w1 * h1
                        area2 = w2 * h2
                        
                        # 检查重叠比例
                        if (overlap_area / min(area1, area2) > overlap_threshold or
                            abs(x1 - x2) < min(w1, w2) * 0.5):  # 水平距离很近
                            
                            # 合并轮廓
                            current_x = min(current_x, x2)
                            current_y = min(current_y, y2)
                            current_right = max(current_right, x2 + w2)
                            current_bottom = max(current_bottom, y2 + h2)
                            
                            used[j] = True
                
                # 添加合并后的轮廓
                merged_width = current_right - current_x
                merged_height = current_bottom - current_y
                
                # 创建合并后的边界框
                merged_contour = np.array([[[current_x, current_y]], 
                                         [[current_x + merged_width, current_y]],
                                         [[current_x + merged_width, current_y + merged_height]],
                                         [[current_x, current_y + merged_height]]])
                
                merged.append((current_x, current_y, merged_width, merged_height, merged_contour))
                used[i] = True
            
            return merged
            
        except Exception as e:
            logger.warning(f"轮廓合并失败: {e}")
            return contours

    def _extract_digit_roi(self, binary_image: np.ndarray, x: int, y: int, w: int, h: int, 
                          img_height: int, img_width: int) -> Optional[np.ndarray]:
        """
        智能提取数字ROI区域
        """
        try:
            # 动态填充大小
            padding_scale = min(0.1, max(0.02, min(w, h) / 100))
            pad = max(2, int(min(w, h) * padding_scale))
            
            x_pad = max(0, x - pad)
            y_pad = max(0, y - pad)
            w_pad = min(img_width - x_pad, w + 2 * pad)
            h_pad = min(img_height - y_pad, h + 2 * pad)
            
            # 提取ROI
            roi = binary_image[y_pad:y_pad+h_pad, x_pad:x_pad+w_pad]
            
            # 验证ROI质量
            if roi.size == 0:
                return None
            
            # 检查ROI是否包含足够的像素
            pixel_count = np.count_nonzero(roi)
            total_pixels = roi.size
            pixel_ratio = pixel_count / total_pixels if total_pixels > 0 else 0
            
            if pixel_ratio < 0.05 or pixel_ratio > 0.9:  # 太稀疏或太密集
                return None
            
            return roi
            
        except Exception as e:
            logger.warning(f"ROI提取失败: {e}")
            return None

    def validate_image(self, image_bytes: bytes, max_size: int = 5 * 1024 * 1024) -> bool:
        """
        验证图像是否有效

        Args:
            image_bytes: 图像字节数据
            max_size: 最大文件大小（字节）

        Returns:
            是否为有效图像
        """
        try:
            # 检查文件大小
            if len(image_bytes) > max_size:
                logger.warning(f"图像文件太大: {len(image_bytes)} bytes")
                return False

            # 尝试打开图像
            image = Image.open(io.BytesIO(image_bytes))

            # 检查图像尺寸
            width, height = image.size
            if width > 5000 or height > 5000:
                logger.warning(f"图像尺寸太大: {width}x{height}")
                return False

            if width < 10 or height < 10:
                logger.warning(f"图像尺寸太小: {width}x{height}")
                return False

            return True

        except Exception as e:
            logger.error(f"图像验证失败: {e}")
            return False
