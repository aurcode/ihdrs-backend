# services/model_service.py - 模型服务
import numpy as np
import tensorflow as tf
from tensorflow import keras
import logging
import os
from pathlib import Path

logger = logging.getLogger(__name__)

class ModelService:
    def __init__(self):
        self.models = {}
        self.active_model_id = None
        self.model_metadata = {}
        self.model_input_type = {}

    def load_default_model(self):
        """加载默认模型并预热"""
        try:
            from config import Config
            default_path = Config.DEFAULT_MODEL_PATH

            if os.path.exists(default_path):
                model = keras.models.load_model(default_path)
                self.models[1] = model
                self.active_model_id = 1

                # 检测模型输入类型
                input_shape = model.input_shape
                logger.info(f"模型输入形状: {input_shape}")

                if len(input_shape) == 2 and input_shape[1] == 784:
                    self.model_input_type[1] = 'flatten'
                    logger.info("检测到全连接网络模型（输入: 784）")
                elif len(input_shape) == 4:
                    self.model_input_type[1] = 'cnn'
                    logger.info("检测到CNN模型（输入: 28x28x1）")
                else:
                    self.model_input_type[1] = 'unknown'
                    logger.warning(f"未知的模型输入格式: {input_shape}")

                self.model_metadata[1] = {
                    'name': 'DefaultModel',
                    'version': 'v1.0.0',
                    'path': str(default_path),
                    'input_type': self.model_input_type[1],
                    'input_shape': input_shape
                }

                self._warmup_model(model, self.model_input_type[1])

                logger.info(f"默认模型加载成功: {default_path}")
                return True
            else:
                logger.warning(f"默认模型文件不存在: {default_path}")
                return False

        except Exception as e:
            logger.error(f"加载默认模型失败: {e}")
            return False

    def _warmup_model(self, model, input_type):
        """模型预热"""
        try:
            logger.info("开始模型预热...")

            # 根据输入类型准备dummy数据
            if input_type == 'flatten':
                dummy_input = np.zeros((1, 784), dtype=np.float32)
            elif input_type == 'cnn':
                dummy_input = np.zeros((1, 28, 28, 1), dtype=np.float32)
            else:
                dummy_input = np.zeros((1, 28, 28, 1), dtype=np.float32)

            # 执行3次预测进行预热
            for i in range(3):
                _ = model.predict(dummy_input, verbose=0)

            logger.info("模型预热完成")
        except Exception as e:
            logger.warning(f"模型预热失败: {e}")

    def predict(self, image, model_id=None):
        """执行预测 - 自动适配输入格式"""
        try:
            target_model_id = model_id if model_id is not None else self.active_model_id

            if target_model_id is None or target_model_id not in self.models:
                logger.error(f"模型不可用: {target_model_id}")
                return None

            model = self.models[target_model_id]
            input_type = self.model_input_type.get(target_model_id, 'unknown')

            # 准备输入数据
            if input_type == 'flatten':
                if len(image.shape) == 2:
                    processed_input = image.reshape(1, 784)
                elif len(image.shape) == 3:
                    processed_input = image.reshape(1, 784)
                else:
                    processed_input = image.reshape(1, -1)
                logger.debug(f"使用扁平化输入: {processed_input.shape}")

            elif input_type == 'cnn':
                if len(image.shape) == 2:
                    processed_input = image.reshape(1, 28, 28, 1)
                elif len(image.shape) == 3:
                    processed_input = image.reshape(1, 28, 28, 1)
                elif len(image.shape) == 4:
                    processed_input = image
                else:
                    logger.error(f"无法处理的输入形状: {image.shape}")
                    return None
                logger.debug(f"使用CNN输入: {processed_input.shape}")

            else:
                logger.warning("未知模型类型，尝试自动适配...")
                if len(image.shape) == 2:
                    processed_input = image.reshape(1, 28, 28, 1)
                else:
                    processed_input = image

            # 执行预测
            predictions = model.predict(processed_input, verbose=0)

            # 处理预测结果
            if len(predictions.shape) > 1:
                confidence_scores = predictions[0]
            else:
                confidence_scores = predictions

            predicted_digit = np.argmax(confidence_scores)
            confidence = float(confidence_scores[predicted_digit])

            result = {
                'digit': int(predicted_digit),
                'confidence': confidence,
                'all_probabilities': confidence_scores.tolist(),
                'model_id': target_model_id,
                'input_type': input_type
            }

            logger.debug(f"预测完成 - 数字: {predicted_digit}, 置信度: {confidence:.4f}")
            return result

        except Exception as e:
            logger.error(f"预测失败: {e}", exc_info=True)
            return None

    def get_active_model_id(self):
        return self.active_model_id

    def list_models(self):
        models_info = []
        for model_id in self.models.keys():
            info = {
                'model_id': model_id,
                'is_active': model_id == self.active_model_id,
                'metadata': self.model_metadata.get(model_id, {}),
                'input_type': self.model_input_type.get(model_id, 'unknown')
            }
            models_info.append(info)

        return {
            'total_models': len(self.models),
            'active_model_id': self.active_model_id,
            'models': models_info
        }

    def save_model(self, model, model_name, model_metadata=None):
        """保存训练好的模型"""
        try:
            from config import Config
            model_path = Config.MODEL_PATH / f"{model_name}.h5"

            # 保存模型文件
            model.save(model_path)

            # 生成模型ID（简单自增）
            model_id = max(self.models.keys()) + 1 if self.models else 2

            # 加载到内存
            self.models[model_id] = model

            # 检测输入类型
            input_shape = model.input_shape
            if len(input_shape) == 2 and input_shape[1] == 784:
                self.model_input_type[model_id] = 'flatten'
            elif len(input_shape) == 4:
                self.model_input_type[model_id] = 'cnn'
            else:
                self.model_input_type[model_id] = 'unknown'

            # 保存元数据
            self.model_metadata[model_id] = {
                'name': model_name,
                'path': str(model_path),
                'input_type': self.model_input_type[model_id],
                'input_shape': input_shape,
                **(model_metadata or {})
            }

            logger.info(f"模型保存成功: {model_path}, ID: {model_id}")
            return model_id

        except Exception as e:
            logger.error(f"保存模型失败: {e}")
            return None