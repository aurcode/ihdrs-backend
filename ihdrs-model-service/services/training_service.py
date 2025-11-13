# services/training_service.py
import os

import tensorflow as tf
from tensorflow import keras
import numpy as np
import time
import logging
import requests
import json
from models_code.cnn_model import create_cnn_model
from config import Config

logger = logging.getLogger(__name__)

class TrainingService:
    def __init__(self, task_id, springboot_url='http://localhost:8080'):
        self.task_id = task_id
        self.springboot_url = springboot_url
        self.is_cancelled = False

    def start_training(self, training_config_str, dataset_config_str):
        """根据 SpringBoot 配置进行训练"""
        try:
            # 解析配置
            training_config = json.loads(training_config_str)
            dataset_config = json.loads(dataset_config_str)

            # 读取训练配置
            epochs = int(training_config.get("epochs", 10))
            batch_size = int(training_config.get("batchsize", 32))
            learning_rate = float(training_config.get("learningrate", 0.001))
            optimizer_name = training_config.get("optimizer", "adam").lower()
            model_type = training_config.get("modeltype", "cnn").lower()

            # 读取数据集配置
            relative_path = dataset_config["file_path"]
            dataset_type = dataset_config.get("dataset_type", "").lower()
            dataset_path = os.path.join(Config.SPRINGBOOT_UPLOAD_DATASET_ROOT, relative_path)

            num_classes = int(dataset_config.get("num_classes", 10))

            if relative_path == "./datasets/mnist/dataset.zip":
                print("使用内置 MNIST 数据集进行训练")

                (x_train, y_train), (x_test, y_test) = tf.keras.datasets.mnist.load_data()

                # MNIST -> 28x28x1
                x_train = x_train.reshape((-1, 28, 28, 1)).astype("float32") / 255.0
                x_test = x_test.reshape((-1, 28, 28, 1)).astype("float32") / 255.0

                y_train = tf.one_hot(y_train, 10)
                y_test = tf.one_hot(y_test, 10)

                train_ds = tf.data.Dataset.from_tensor_slices((x_train, y_train)).batch(batch_size)
                test_ds = tf.data.Dataset.from_tensor_slices((x_test, y_test)).batch(batch_size)

                # 选择模型
                if model_type == "cnn":
                    model = create_cnn_model(input_shape=(28,28,1), num_classes=10)
                elif model_type == "advanced_cnn":
                    model = create_cnn_model(input_shape=(28,28,1), num_classes=10)
                else:
                    raise ValueError(f"MNIST 不支持该模型类型: {model_type}")

            else:
                print("加载用户数据集：", dataset_path)

                img_w = int(dataset_config.get("image_width"))
                img_h = int(dataset_config.get("image_height"))

                train_dir = os.path.join(dataset_path, "train")
                test_dir = os.path.join(dataset_path, "test")

                # 加载训练数据
                train_ds = tf.keras.utils.image_dataset_from_directory(
                    train_dir,
                    image_size=(img_h, img_w),
                    batch_size=batch_size,
                    shuffle=True
                )

                # 测试集是否存在
                if os.path.exists(test_dir) and any(os.scandir(test_dir)):
                    test_ds = tf.keras.utils.image_dataset_from_directory(
                        test_dir,
                        image_size=(img_h, img_w),
                        batch_size=batch_size,
                        shuffle=False
                    )
                else:
                    print("未找到 test 目录，将自动从 train 划分 20% 作为验证集")
                    train_ds_size = train_ds.cardinality().numpy()
                    val_size = max(1, int(train_ds_size * 0.2))

                    test_ds = train_ds.take(val_size)
                    train_ds = train_ds.skip(val_size)

                # 标准化
                train_ds = train_ds.map(lambda x, y: (x/255.0, tf.one_hot(y, num_classes)))
                test_ds = test_ds.map(lambda x, y: (x/255.0, tf.one_hot(y, num_classes)))

                # 选择模型
                if model_type == "cnn":
                    model = create_cnn_model(input_shape=(img_h, img_w, 3), num_classes=num_classes)
                elif model_type == "advanced_cnn":
                    model = create_cnn_model(input_shape=(img_h, img_w, 3), num_classes=num_classes)
                else:
                    raise ValueError(f"不支持的模型类型: {model_type}")

            optimizer = {
                "adam": keras.optimizers.Adam(learning_rate),
                "sgd": keras.optimizers.SGD(learning_rate),
                "rmsprop": keras.optimizers.RMSprop(learning_rate),
            }.get(optimizer_name)

            if optimizer is None:
                raise ValueError(f"不支持的优化器: {optimizer_name}")

            model.compile(
                optimizer=optimizer,
                loss="categorical_crossentropy",
                metrics=["accuracy"]
            )

            # 回调，用于向 SpringBoot 发送进度
            class ProgressCallback(keras.callbacks.Callback):
                def __init__(self, service):
                    super().__init__()
                    self.service = service

                def on_epoch_end(self, epoch, logs=None):
                    progress = (epoch + 1) / epochs * 100
                    self.service.report_progress({
                        "currentEpoch": epoch + 1,
                        "progress": progress,
                        "loss": float(logs.get("loss", 0)),
                        "accuracy": float(logs.get("accuracy", 0)),
                        "valLoss": float(logs.get("val_loss", 0)),
                        "valAccuracy": float(logs.get("val_accuracy", 0)),
                    })

            # 开始训练
            history = model.fit(
                train_ds,
                validation_data=test_ds,
                epochs=epochs,
                callbacks=[ProgressCallback(self)],
                verbose=1
            )

            # 保存模型
            model_path = f"models/model_{self.task_id}_{int(time.time())}.h5"
            model.save(model_path)

            # 最终评估
            loss, accuracy = model.evaluate(test_ds, verbose=0)

            self.report_completion({
                "finalAccuracy": float(accuracy),
                "finalLoss": float(loss),
                "modelPath": model_path,
                "trainingSamples": int(train_ds.cardinality().numpy()),
                "testSamples": int(test_ds.cardinality().numpy()),
                "modelSize": int(os.path.getsize(model_path))
            })

        except Exception as e:
            logger.error(f"训练失败: {e}", exc_info=True)
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
