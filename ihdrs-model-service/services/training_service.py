# services/training_service.py
import os

import tensorflow as tf
from tensorflow import keras
import numpy as np
import time
import logging
import requests
import json
from models_code.cnn_model import create_cnn_model, create_advanced_cnn_model

logger = logging.getLogger(__name__)

class TrainingService:
    def __init__(self, task_id, springboot_url='http://localhost:8080'):
        self.task_id = task_id
        self.springboot_url = springboot_url
        self.is_cancelled = False

    def start_training(self, training_config_str, dataset_config_str):
        """启动训练"""
        try:
            # 解析配置
            training_config = json.loads(training_config_str) if isinstance(training_config_str, str) else training_config_str
            dataset_config = json.loads(dataset_config_str) if isinstance(dataset_config_str, str) else dataset_config_str
            print("training_config_str=", training_config_str)
            print("training_config=", training_config)


            epochs = training_config.get('epochs', 10)
            batch_size = training_config.get('batchsize', 32)
            learning_rate = float(training_config.get('learningrate', 0.001))

            logger.info(f"开始训练任务 {self.task_id}")

            # 加载数据
            (x_train, y_train), (x_test, y_test) = keras.datasets.mnist.load_data()

            # 数据预处理
            x_train = x_train.reshape(x_train.shape[0], 28, 28, 1).astype('float32') / 255.0
            x_test = x_test.reshape(x_test.shape[0], 28, 28, 1).astype('float32') / 255.0
            y_train = keras.utils.to_categorical(y_train, 10)
            y_test = keras.utils.to_categorical(y_test, 10)

            # 创建模型
            model = create_cnn_model()

            # 自定义回调，报告训练进度
            class ProgressCallback(keras.callbacks.Callback):
                def __init__(self, service):
                    super().__init__()
                    self.service = service

                def on_epoch_end(self, epoch, logs=None):
                    if self.service.is_cancelled:
                        self.model.stop_training = True
                        return

                    progress = ((epoch + 1) / self.params['epochs']) * 100

                    # 向SpringBoot报告进度
                    progress_data = {
                        'currentEpoch': epoch + 1,
                        'progress': progress,
                        'loss': float(logs.get('loss', 0)),
                        'accuracy': float(logs.get('accuracy', 0)),
                        'valLoss': float(logs.get('val_loss', 0)),
                        'valAccuracy': float(logs.get('val_accuracy', 0))
                    }

                    self.service.report_progress(progress_data)

            # 训练模型
            history = model.fit(
                x_train, y_train,
                batch_size=batch_size,
                epochs=epochs,
                validation_data=(x_test, y_test),
                callbacks=[ProgressCallback(self)],
                verbose=1
            )

            if not self.is_cancelled:
                # 保存模型
                model_path = f"models/trained_model_{self.task_id}_{int(time.time())}.h5"
                model.save(model_path)

                # 评估模型
                test_loss, test_accuracy = model.evaluate(x_test, y_test, verbose=0)

                model_size = os.path.getsize(model_path)

                # 报告完成
                result_data = {
                    'finalAccuracy': float(test_accuracy),
                    'finalLoss': float(test_loss),
                    'modelPath': model_path,
                    'trainingSamples': len(x_train),
                    'testSamples': len(x_test),
                    'modelSize': model_size
                }

                self.report_completion(result_data)
                logger.info(f"训练任务 {self.task_id} 完成，准确率: {test_accuracy:.4f}")

        except Exception as e:
            logger.error(f"训练任务 {self.task_id} 失败: {e}", exc_info=True)
            self.report_failure(str(e))

    def report_progress(self, progress_data):
        """向SpringBoot报告训练进度"""
        try:
            url = f"{self.springboot_url}/api/training/tasks/{self.task_id}/progress"
            logger.warning(f"{self.springboot_url}/api/training/tasks/{self.task_id}/progress")
            headers = {'Content-Type': 'application/json'}
            response = requests.post(url, json=progress_data, headers=headers, timeout=5)

            if response.status_code != 200:
                logger.warning(f"报告进度失败: {response.status_code}")
        except Exception as e:
            logger.error(f"报告进度异常: {e}")

    def report_completion(self, result_data):
        """向SpringBoot报告训练完成"""
        try:
            url = f"{self.springboot_url}/api/training/tasks/{self.task_id}/complete"
            headers = {'Content-Type': 'application/json'}
            response = requests.post(url, json=result_data, headers=headers, timeout=5)

            if response.status_code != 200:
                logger.warning(f"报告完成失败: {response.status_code}")
        except Exception as e:
            logger.error(f"报告完成异常: {e}")

    def report_failure(self, error_message):
        """向SpringBoot报告训练失败"""
        try:
            url = f"{self.springboot_url}/api/training/tasks/{self.task_id}/fail"
            headers = {'Content-Type': 'application/json'}
            data = {'errorMessage': error_message}
            response = requests.post(url, json=data, headers=headers, timeout=5)

            if response.status_code != 200:
                logger.warning(f"报告失败状态失败: {response.status_code}")
        except Exception as e:
            logger.error(f"报告失败状态异常: {e}")
