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

        # 6. 数据增强（可选）
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
        分割连续数字（为连续数字识别功能准备）

        Args:
            image_bytes: 包含多个数字的图像字节数据

        Returns:
            分割出的单个数字图像列表
        """
        try:
            # 预处理
            image = Image.open(io.BytesIO(image_bytes))
            if image.mode != 'RGB':
                image = image.convert('RGB')

            image_array = np.array(image)
            image_gray = cv2.cvtColor(image_array, cv2.COLOR_RGB2GRAY)

            # 二值化
            _, binary = cv2.threshold(image_gray, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)

            # 查找轮廓
            contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

            if not contours:
                return []

            # 按x坐标排序轮廓（从左到右）
            contours = sorted(contours, key=lambda c: cv2.boundingRect(c)[0])

            digits = []
            for contour in contours:
                # 获取边界框
                x, y, w, h = cv2.boundingRect(contour)

                # 过滤太小的轮廓
                if w < 10 or h < 10:
                    continue

                # 提取数字区域
                digit_roi = binary[y:y+h, x:x+w]

                # 调整大小到28x28
                resized_digit = cv2.resize(digit_roi, self.target_size, interpolation=cv2.INTER_AREA)

                # 归一化
                normalized_digit = resized_digit.astype(np.float32) / 255.0

                digits.append(normalized_digit)

            return digits

        except Exception as e:
            logger.error(f"数字分割失败: {e}")
            return []

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