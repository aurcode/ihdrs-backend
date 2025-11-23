# services/image_processor.py

import cv2
import numpy as np
from PIL import Image
import io
import logging
from typing import Optional, Tuple, List, Dict, Any
import time
from functools import lru_cache

logger = logging.getLogger(__name__)

class ImageProcessor:
    """Enhanced image processing service for improved multi-character recognition"""
    
    def __init__(self, target_size: Tuple[int, int] = (28, 28)):
        self.target_size = target_size
        self.min_digit_area = 30
        self.max_digit_area_ratio = 0.4
        self.min_aspect_ratio = 0.2
        self.max_aspect_ratio = 5.0
        
    def preprocess_for_recognition(self, image_bytes: bytes) -> Optional[np.ndarray]:
        """Enhanced preprocessing for single digit recognition"""
        try:
            # Load and validate image
            image = self._load_image(image_bytes)
            if image is None:
                return None
                
            # Convert to grayscale
            gray = self._convert_to_grayscale(image)
            
            # Apply enhanced preprocessing pipeline
            processed = self._enhanced_preprocess_pipeline(gray)
            
            return processed
            
        except Exception as e:
            logger.error(f"Preprocessing failed: {e}")
            return None
    
    def segment_digits(self, image_bytes: bytes) -> List[np.ndarray]:
        """Enhanced multi-digit segmentation with improved accuracy"""
        try:
            start_time = time.time()
            
            # Load and validate image
            image = self._load_image(image_bytes)
            if image is None:
                return []
            
            # Convert to grayscale
            gray = self._convert_to_grayscale(image)
            
            # Apply enhanced multi-digit preprocessing
            preprocessed = self._enhanced_multidigit_preprocessing(gray)
            
            # Detect and segment characters
            digit_regions = self._detect_characters(preprocessed)
            
            # Process each digit
            processed_digits = []
            for region in digit_regions:
                processed_digit = self._process_digit_region(region)
                if processed_digit is not None:
                    processed_digits.append(processed_digit)
            
            processing_time = (time.time() - start_time) * 1000
            logger.info(f"Segmented {len(processed_digits)} digits in {processing_time:.2f}ms")
            
            return processed_digits
            
        except Exception as e:
            logger.error(f"Multi-digit segmentation failed: {e}", exc_info=True)
            return []
    
    def _load_image(self, image_bytes: bytes) -> Optional[np.ndarray]:
        """Load and validate image from bytes"""
        try:
            if not self.validate_image(image_bytes):
                return None
                
            image = Image.open(io.BytesIO(image_bytes))
            
            # Handle different image modes
            if image.mode == 'RGBA':
                background = Image.new('RGB', image.size, (255, 255, 255))
                background.paste(image, mask=image.split()[-1])
                image = background
            elif image.mode != 'RGB':
                image = image.convert('RGB')
            
            return np.array(image)
            
        except Exception as e:
            logger.error(f"Image loading failed: {e}")
            return None
    
    def _convert_to_grayscale(self, image: np.ndarray) -> np.ndarray:
        """Convert image to grayscale with enhanced quality"""
        if len(image.shape) == 3:
            # Use weighted conversion for better contrast
            gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
        else:
            gray = image.copy()
        
        return gray
    
    def _enhanced_preprocess_pipeline(self, gray: np.ndarray) -> np.ndarray:
        """Enhanced preprocessing for single digits"""
        # Apply noise reduction
        denoised = cv2.bilateralFilter(gray, 9, 75, 75)
        
        # Adaptive thresholding
        binary = cv2.adaptiveThreshold(
            denoised, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
            cv2.THRESH_BINARY_INV, 11, 2
        )
        
        # Extract ROI
        roi = self._extract_enhanced_roi(binary)
        
        # Resize and normalize
        resized = cv2.resize(roi, self.target_size, interpolation=cv2.INTER_AREA)
        normalized = resized.astype(np.float32) / 255.0
        
        return normalized
    
    def _enhanced_multidigit_preprocessing(self, gray: np.ndarray) -> np.ndarray:
        """Enhanced preprocessing specifically for multi-digit images"""
        # Multi-scale noise reduction
        blurred1 = cv2.GaussianBlur(gray, (3, 3), 0)
        blurred2 = cv2.GaussianBlur(gray, (5, 5), 0)
        blurred = cv2.addWeighted(blurred1, 0.5, blurred2, 0.5, 0)
        
        # Contrast enhancement
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
        enhanced = clahe.apply(blurred)
        
        # Adaptive thresholding with multiple parameters
        binary1 = cv2.adaptiveThreshold(
            enhanced, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
            cv2.THRESH_BINARY_INV, 15, 3
        )
        
        binary2 = cv2.adaptiveThreshold(
            enhanced, 255, cv2.ADAPTIVE_THRESH_MEAN_C,
            cv2.THRESH_BINARY_INV, 21, 5
        )
        
        # Combine thresholds
        binary = cv2.bitwise_or(binary1, binary2)
        
        # Remove border artifacts
        h, w = binary.shape
        border_size = 3
        cv2.rectangle(binary, (0, 0), (w, h), (0, 0, 0), border_size * 2)
        
        return binary
    
    def _detect_characters(self, binary: np.ndarray) -> List[np.ndarray]:
        """Enhanced character detection with multiple techniques"""
        regions = []
        
        # Method 1: Connected components
        regions.extend(self._connected_components_analysis(binary))
        
        # Method 2: Contour analysis
        regions.extend(self._contour_based_segmentation(binary))
        
        # Method 3: Watershed for touching characters
        if len(regions) <= 1:  # If few regions found, try watershed
            regions.extend(self._watershed_segmentation(binary))
        
        # Remove duplicates and filter
        unique_regions = self._remove_duplicate_regions(regions)
        filtered_regions = self._filter_regions(unique_regions)
        
        # Sort left to right
        filtered_regions.sort(key=lambda x: x[0])
        
        return [binary[y:y+h, x:x+w] for x, y, w, h in filtered_regions]
    
    def _connected_components_analysis(self, binary: np.ndarray) -> List[Tuple[int, int, int, int]]:
        """Analyze connected components to find character regions"""
        regions = []
        
        # Find connected components
        num_labels, labels, stats, centroids = cv2.connectedComponentsWithStats(binary, connectivity=8)
        
        h_img, w_img = binary.shape
        img_area = h_img * w_img
        
        for i in range(1, num_labels):  # Skip background (label 0)
            x, y, w, h, area = stats[i]
            
            # Apply intelligent filtering
            if not self._is_valid_character_region(x, y, w, h, area, img_area):
                continue
            
            regions.append((x, y, w, h))
        
        return regions
    
    def _contour_based_segmentation(self, binary: np.ndarray) -> List[Tuple[int, int, int, int]]:
        """Advanced contour-based character segmentation"""
        regions = []
        
        # Apply morphological operations to separate touching characters
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3))
        
        # Try different erosion levels
        for erosion_iter in range(0, 3):
            if erosion_iter > 0:
                eroded = cv2.erode(binary, kernel, iterations=erosion_iter)
            else:
                eroded = binary
            
            # Find contours
            contours, _ = cv2.findContours(eroded, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            h_img, w_img = binary.shape
            img_area = h_img * w_img
            
            for contour in contours:
                x, y, w, h = cv2.boundingRect(contour)
                area = w * h
                
                if self._is_valid_character_region(x, y, w, h, area, img_area):
                    regions.append((x, y, w, h))
        
        return regions
    
    def _watershed_segmentation(self, binary: np.ndarray) -> List[Tuple[int, int, int, int]]:
        """Watershed algorithm for separating touching characters"""
        regions = []
        
        # Distance transform
        dist_transform = cv2.distanceTransform(binary, cv2.DIST_L2, 5)
        
        # Find local maxima (potential character centers)
        _, sure_fg = cv2.threshold(dist_transform, 0.3 * dist_transform.max(), 255, 0)
        sure_fg = np.uint8(sure_fg)
        
        # Find unknown region
        sure_bg = cv2.dilate(binary, np.ones((3, 3), np.uint8), iterations=3)
        unknown = cv2.subtract(sure_bg, sure_fg)
        
        # Marker labeling
        _, markers = cv2.connectedComponents(sure_fg)
        markers = markers + 1
        markers[unknown == 255] = 0
        
        # Apply watershed
        binary_3ch = cv2.cvtColor(binary, cv2.COLOR_GRAY2BGR)
        markers = cv2.watershed(binary_3ch, markers)
        
        # Extract regions from markers
        unique_markers = np.unique(markers)
        h_img, w_img = binary.shape
        
        for marker in unique_markers:
            if marker <= 1:  # Skip background and border
                continue
            
            # Find region for this marker
            region_mask = (markers == marker).astype(np.uint8) * 255
            contours, _ = cv2.findContours(region_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            if contours:
                x, y, w, h = cv2.boundingRect(contours[0])
                area = w * h
                
                if self._is_valid_character_region(x, y, w, h, area, h_img * w_img):
                    regions.append((x, y, w, h))
        
        return regions
    
    def _is_valid_character_region(self, x: int, y: int, w: int, h: int, area: int, img_area: int) -> bool:
        """Intelligent filtering for character regions"""
        # Size filters
        if area < self.min_digit_area or area > img_area * self.max_digit_area_ratio:
            return False
        
        # Aspect ratio filters
        aspect_ratio = w / h if h > 0 else float('inf')
        if aspect_ratio < self.min_aspect_ratio or aspect_ratio > self.max_aspect_ratio:
            return False
        
        # Minimum dimension filters
        if w < 4 or h < 8:
            return False
        
        # Density filter (check if region has enough pixel density)
        # This will be checked later when we have the actual region
        
        return True
    
    def _remove_duplicate_regions(self, regions: List[Tuple[int, int, int, int]]) -> List[Tuple[int, int, int, int]]:
        """Remove overlapping regions"""
        if not regions:
            return []
        
        # Sort by area (largest first)
        regions_with_area = [(x, y, w, h, w * h) for x, y, w, h in regions]
        regions_with_area.sort(key=lambda x: x[4], reverse=True)
        
        unique_regions = []
        
        for x, y, w, h, area in regions_with_area:
            is_duplicate = False
            
            for ux, uy, uw, uh in unique_regions:
                # Calculate overlap
                overlap_x = max(0, min(x + w, ux + uw) - max(x, ux))
                overlap_y = max(0, min(y + h, uy + uh) - max(y, uy))
                overlap_area = overlap_x * overlap_y
                
                # If overlap is significant, mark as duplicate
                if overlap_area > 0.5 * min(area, uw * uh):
                    is_duplicate = True
                    break
            
            if not is_duplicate:
                unique_regions.append((x, y, w, h))
        
        return unique_regions
    
    def _filter_regions(self, regions: List[Tuple[int, int, int, int]]) -> List[Tuple[int, int, int, int]]:
        """Apply additional filtering to regions"""
        filtered = []
        
        for x, y, w, h in regions:
            # Additional quality checks can be added here
            filtered.append((x, y, w, h))
        
        return filtered
    
    def _extract_enhanced_roi(self, binary: np.ndarray) -> np.ndarray:
        """Enhanced ROI extraction"""
        contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        
        if not contours:
            return binary
        
        # Find the largest contour
        largest_contour = max(contours, key=cv2.contourArea)
        x, y, w, h = cv2.boundingRect(largest_contour)
        
        # Add intelligent padding
        margin = max(2, min(w, h) // 10)
        x = max(0, x - margin)
        y = max(0, y - margin)
        w = min(binary.shape[1] - x, w + 2 * margin)
        h = min(binary.shape[0] - y, h + 2 * margin)
        
        roi = binary[y:y+h, x:x+w]
        
        # Ensure minimum size
        if roi.shape[0] < 10 or roi.shape[1] < 10:
            return binary
        
        return roi
    
    def _process_digit_region(self, region: np.ndarray) -> Optional[np.ndarray]:
        """Process individual digit region"""
        try:
            # Apply dilation to restore thickness lost during segmentation
            kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2, 2))
            region = cv2.dilate(region, kernel, iterations=1)
            
            # Resize with padding
            processed = self._resize_with_pad(region)
            
            return processed
            
        except Exception as e:
            logger.error(f"Digit region processing failed: {e}")
            return None
    
    def _resize_with_pad(self, image: np.ndarray) -> np.ndarray:
        """Enhanced resize with padding and centering"""
        # Ensure the image is not empty
        if image.size == 0:
            return np.zeros(self.target_size, dtype=np.float32)
        
        h, w = image.shape
        
        # Apply slight dilation to prevent stroke loss
        kernel = np.ones((2, 2), np.uint8)
        image = cv2.dilate(image, kernel, iterations=1)
        
        # Calculate scaling to fit in 20x20 inner area
        inner_size = 20
        scale = min(inner_size / h, inner_size / w) if max(h, w) > 0 else 1.0
        
        new_w = max(1, int(w * scale))
        new_h = max(1, int(h * scale))
        
        # Resize image
        resized = cv2.resize(image, (new_w, new_h), interpolation=cv2.INTER_AREA)
        
        # Create canvas
        canvas = np.zeros(self.target_size, dtype=np.uint8)
        
        # Center the resized image
        y_offset = (self.target_size[0] - new_h) // 2
        x_offset = (self.target_size[1] - new_w) // 2
        
        canvas[y_offset:y_offset+new_h, x_offset:x_offset+new_w] = resized
        
        # Center of mass adjustment
        centered = self._center_image(canvas)
        
        # Normalize
        normalized = centered.astype(np.float32) / 255.0
        
        return normalized
    
    def _center_image(self, image: np.ndarray) -> np.ndarray:
        """Center image using center of mass"""
        h, w = image.shape
        
        # Calculate moments
        moments = cv2.moments(image)
        
        if moments["m00"] == 0:
            return image
        
        # Calculate center of mass
        cx = moments["m10"] / moments["m00"]
        cy = moments["m01"] / moments["m00"]
        
        # Calculate shift
        shift_x = int(round(w / 2.0 - cx))
        shift_y = int(round(h / 2.0 - cy))
        
        # Apply translation
        M = np.float32([[1, 0, shift_x], [0, 1, shift_y]])
        centered = cv2.warpAffine(image, M, (w, h))
        
        return centered
    
    def validate_image(self, image_bytes: bytes, max_size: int = 5 * 1024 * 1024) -> bool:
        """Enhanced image validation"""
        try:
            # Check file size
            if len(image_bytes) > max_size:
                logger.warning(f"Image too large: {len(image_bytes)} bytes")
                return False
            
            # Try to open image
            image = Image.open(io.BytesIO(image_bytes))
            
            # Check dimensions
            width, height = image.size
            if width > 5000 or height > 5000:
                logger.warning(f"Image dimensions too large: {width}x{height}")
                return False
            
            if width < 10 or height < 10:
                logger.warning(f"Image dimensions too small: {width}x{height}")
                return False
            
            return True
            
        except Exception as e:
            logger.error(f"Image validation failed: {e}")
            return False
