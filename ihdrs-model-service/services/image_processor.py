# services/image_processor.py

import cv2
import numpy as np
from PIL import Image
import io
import logging
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

    def segment_digits(self, image_bytes: bytes) -> List[np.ndarray]:
        """
        分割连续数字（增强版：抗阴影、抗边框、分离粘连）
        """
        try:
            # 1. 基础加载
            image = Image.open(io.BytesIO(image_bytes))
            if image.mode != 'RGB':
                image = image.convert('RGB')
            image_array = np.array(image)

            # 转灰度
            if len(image_array.shape) == 3:
                gray = cv2.cvtColor(image_array, cv2.COLOR_RGB2GRAY)
            else:
                gray = image_array

            # 2. 预处理优化
            # 2.1 高斯模糊去噪
            blurred = cv2.GaussianBlur(gray, (5, 5), 0)

            # 2.2 自适应阈值 (关键修改：比 OTSU 更适合处理光照不均的手写图)
            # block_size=11, C=2 是经验值，将图像转为黑底白字
            binary = cv2.adaptiveThreshold(
                blurred, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                cv2.THRESH_BINARY_INV, 19, 5
            )

            # 2.3 清除边缘 (关键修改：防止图片边框被当成一个大轮廓)
            h_img, w_img = binary.shape
            border_size = 5
            cv2.rectangle(binary, (0, 0), (w_img, h_img), (0, 0, 0), border_size * 2)

            # 2.4 形态学操作 - 腐蚀 (关键修改：用于分离粘连的数字)
            # 在黑底白字模式下，腐蚀会“削细”白色笔画，从而断开连接
            kernel = np.ones((3, 3), np.uint8)
            # 这里的 iterations 可以调整，如果数字很细则设为 0 或 1，如果很粗且粘连则设为 2
            eroded = cv2.erode(binary, kernel, iterations=1)

            # 3. 查找轮廓
            # 使用 RETR_EXTERNAL 查找外轮廓
            contours, _ = cv2.findContours(eroded, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            # cv2.imwrite("debug_binary.jpg", binary)
            # cv2.imwrite("debug_eroded.jpg", eroded)

            if not contours:
                logger.warning("未检测到任何轮廓")
                return []

            # 4. 轮廓筛选与提取
            digits = []
            bounding_boxes = []

            image_area = h_img * w_img

            for contour in contours:
                x, y, w, h = cv2.boundingRect(contour)
                area = w * h

                # 过滤 1: 噪点 (太小)
                if area < 50 or w < 5 or h < 10:
                    continue

                # 过滤 2: 边框/背景 (太大)
                # 如果一个轮廓占了全图的 50% 以上，它肯定不是单个数字，而是背景或边框
                if area > image_area * 0.5:
                    logger.info(f"忽略过大轮廓: {w}x{h} (可能是背景)")
                    continue

                bounding_boxes.append((x, y, w, h, contour))

            # 按 x 坐标排序 (从左到右)
            bounding_boxes.sort(key=lambda b: b[0])

            for x, y, w, h, contour in bounding_boxes:
                # 提取 ROI (注意：回到 binary 原图提取，而不是腐蚀后的图，以免笔画缺失)
                # 适当外扩几个像素
                pad = 4
                x_pad = max(0, x - pad)
                y_pad = max(0, y - pad)
                w_pad = min(w_img - x_pad, w + 2 * pad)
                h_pad = min(h_img - y_pad, h + 2 * pad)

                digit_roi = binary[y_pad:y_pad+h_pad, x_pad:x_pad+w_pad]

                # 使用之前定义的保持比例缩放方法
                processed_digit = self._resize_with_pad(digit_roi)
                digits.append(processed_digit)

            logger.info(f"成功分割出 {len(digits)} 个数字")
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