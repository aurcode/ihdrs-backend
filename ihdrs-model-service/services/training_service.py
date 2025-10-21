# services/training_service.py
import tensorflow as tf
from tensorflow import keras
import numpy as np
import time
import logging
from models_code.cnn_model import create_cnn_model, create_advanced_cnn_model

logger = logging.getLogger(__name__)

class TrainingService:
    """训练服务类"""

    def __init__(self):
        self.training_status = {
            'is_training': False,
            'current_epoch': 0,
            'total_epochs': 0,
            'current_loss': 0.0,
            'current_accuracy': 0.0,
            'start_time': None
        }

    def validate_training_config(self, config):
        """验证训练配置"""
        required_fields = ['epochs', 'batch_size', 'learning_rate']
        for field in required_fields:
            if field not in config:
                return f"缺少必需字段: {field}"

        if not isinstance(config['epochs'], int) or config['epochs'] <= 0:
            return "epochs必须是正整数"

        if not isinstance(config['batch_size'], int) or config['batch_size'] <= 0:
            return "batch_size必须是正整数"

        if not isinstance(config['learning_rate'], (int, float)) or config['learning_rate'] <= 0:
            return "learning_rate必须是正数"

        return None

    def start_training(self, config):
        """启动训练任务"""
        try:
            self.training_status['is_training'] = True
            self.training_status['total_epochs'] = config['epochs']

            # 加载MNIST数据集
            (x_train, y_train), (x_test, y_test) = keras.datasets.mnist.load_data()

            # 数据预处理
            x_train = x_train.reshape(x_train.shape[0], 28, 28, 1).astype('float32') / 255.0
            x_test = x_test.reshape(x_test.shape[0], 28, 28, 1).astype('float32') / 255.0

            y_train = keras.utils.to_categorical(y_train, 10)
            y_test = keras.utils.to_categorical(y_test, 10)

            # 创建模型
            model = create_cnn_model()

            # 训练回调
            class TrainingCallback(keras.callbacks.Callback):
                def __init__(self, training_service):
                    self.training_service = training_service

                def on_epoch_end(self, epoch, logs=None):
                    self.training_service.training_status['current_epoch'] = epoch + 1
                    self.training_service.training_status['current_loss'] = logs.get('loss', 0.0)
                    self.training_service.training_status['current_accuracy'] = logs.get('accuracy', 0.0)

            # 开始训练
            model.fit(
                x_train, y_train,
                batch_size=config['batch_size'],
                epochs=config['epochs'],
                validation_data=(x_test, y_test),
                callbacks=[TrainingCallback(self)],
                verbose=1
            )

            # 保存模型
            from config import Config
            model_path = Config.MODEL_PATH / f"trained_model_{int(time.time())}.h5"
            model.save(model_path)

            self.training_status['is_training'] = False
            logger.info(f"训练完成，模型已保存到: {model_path}")

        except Exception as e:
            self.training_status['is_training'] = False
            logger.error(f"训练失败: {e}")

    def get_training_status(self):
        """获取训练状态"""
        return self.training_status.copy()