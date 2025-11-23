import os
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import numpy as np
import time
import logging
import requests
import json
from models_code.model_factory import create_model
from config import Config
from sklearn.metrics import confusion_matrix
from services.tasks_registry import tasks

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
            model_type = training_config.get("modeltype", "cnn")
            use_augmentation = training_config.get("useAugmentation", False)
            augmentation_strength = training_config.get("augmentationStrength", "medium")
            early_stopping_patience = int(training_config.get("earlyStoppingPatience", 0))
            lr_scheduler_type = training_config.get("lrScheduler", "none")

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

                input_shape = (28, 28, 1)

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

                # 数据增强
                if use_augmentation:
                    data_augmentation = self.create_data_augmentation(augmentation_strength)
                    train_ds = train_ds.map(
                        lambda x, y: (data_augmentation(x, training=True), y),
                        num_parallel_calls=tf.data.AUTOTUNE
                    )

                # 标准化
                train_ds = train_ds.map(lambda x, y: (x / 255.0, tf.one_hot(y, num_classes)))
                test_ds = test_ds.map(lambda x, y: (x / 255.0, tf.one_hot(y, num_classes)))

                input_shape = (img_h, img_w, 3)

            # 预取优化
            train_ds = train_ds.prefetch(buffer_size=tf.data.AUTOTUNE)
            test_ds = test_ds.prefetch(buffer_size=tf.data.AUTOTUNE)

            # 创建模型
            model = create_model(model_type, input_shape, num_classes, training_config)

            # 创建优化器
            optimizer = self.create_optimizer(optimizer_name, learning_rate)

            # 编译模型
            loss_fn = training_config.get("lossfunction", "categorical_crossentropy")
            model.compile(
                optimizer=optimizer,
                loss=loss_fn,
                metrics=['accuracy']
            )

            # 创建回调函数
            callbacks = [
                ProgressCallback(self, batch_size=batch_size, learning_rate=learning_rate)
            ]

            # 早停
            if early_stopping_patience > 0:
                callbacks.append(
                    keras.callbacks.EarlyStopping(
                        monitor='val_loss',
                        patience=early_stopping_patience,
                        restore_best_weights=True,
                        verbose=1
                    )
                )

            # 学习率调度
            lr_callback = self.create_lr_scheduler(lr_scheduler_type, learning_rate, epochs)
            if lr_callback:
                callbacks.append(lr_callback)

            # 开始训练
            print(f"开始训练，模型类型: {model_type}, 总轮数: {epochs}")
            history = model.fit(
                train_ds,
                validation_data=test_ds,
                epochs=epochs,
                callbacks=callbacks,
                verbose=1
            )

            if self.is_cancelled:
                self.report_failure("任务已取消")
                return

            # 保存模型
            model_path = f"models/model_{self.task_id}_{int(time.time())}.h5"
            model.save(model_path)
            print(f"模型已保存到: {model_path}")

            # 最终评估
            loss, accuracy = model.evaluate(test_ds, verbose=0)

            # 计算混淆矩阵
            y_true = []
            y_pred = []

            for batch_x, batch_y in test_ds:
                true_labels = tf.argmax(batch_y, axis=-1).numpy()
                preds = model.predict(batch_x, verbose=0)
                pred_labels = np.argmax(preds, axis=-1)

                y_true.extend(true_labels.tolist())
                y_pred.extend(pred_labels.tolist())

            cm = confusion_matrix(y_true, y_pred)
            cm_list = cm.tolist()

            class_names = dataset_config.get("class_names")

            self.report_completion({
                "finalAccuracy": float(accuracy),
                "finalLoss": float(loss),
                "modelPath": model_path,
                "trainingSamples": int(train_ds.cardinality().numpy()),
                "testSamples": int(test_ds.cardinality().numpy()),
                "modelSize": int(os.path.getsize(model_path)),
                "confusionMatrix": cm_list,
                "classNames": class_names,
            })

        except Exception as e:
            logger.error(f"训练失败: {e}", exc_info=True)
            self.report_failure(str(e))

    def create_optimizer(self, optimizer_name, learning_rate):
        """创建优化器"""
        optimizers = {
            "adam": keras.optimizers.Adam(learning_rate),
            "adamw": keras.optimizers.AdamW(learning_rate),
            "sgd": keras.optimizers.SGD(learning_rate, momentum=0.9),
            "rmsprop": keras.optimizers.RMSprop(learning_rate),
            "nadam": keras.optimizers.Nadam(learning_rate),
        }

        optimizer = optimizers.get(optimizer_name)
        if optimizer is None:
            raise ValueError(f"不支持的优化器: {optimizer_name}")

        return optimizer

    def create_lr_scheduler(self, scheduler_type, initial_lr, epochs):
        """创建学习率调度器"""
        if scheduler_type == "none" or not scheduler_type:
            return None

        if scheduler_type == "exponential":
            # 指数衰减
            decay_rate = 0.96
            decay_steps = epochs // 5
            return keras.callbacks.LearningRateScheduler(
                lambda epoch: initial_lr * (decay_rate ** (epoch // decay_steps))
            )

        elif scheduler_type == "cosine":
            # 余弦退火
            return keras.callbacks.LearningRateScheduler(
                lambda epoch: initial_lr * 0.5 * (1 + np.cos(np.pi * epoch / epochs))
            )

        elif scheduler_type == "step":
            # 阶梯衰减
            return keras.callbacks.LearningRateScheduler(
                lambda epoch: initial_lr * (0.5 ** (epoch // (epochs // 3)))
            )

        elif scheduler_type == "reduce_on_plateau":
            # 基于性能的衰减
            return keras.callbacks.ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=3,
                min_lr=initial_lr * 0.001,
                verbose=1
            )

        return None

    def create_data_augmentation(self, strength="medium"):
        """创建数据增强层"""
        if strength == "light":
            return keras.Sequential([
                layers.RandomFlip("horizontal"),
                layers.RandomRotation(0.05),
            ])
        elif strength == "medium":
            return keras.Sequential([
                layers.RandomFlip("horizontal"),
                layers.RandomRotation(0.1),
                layers.RandomZoom(0.1),
            ])
        elif strength == "strong":
            return keras.Sequential([
                layers.RandomFlip("horizontal"),
                layers.RandomFlip("vertical"),
                layers.RandomRotation(0.2),
                layers.RandomZoom(0.2),
                layers.RandomTranslation(0.1, 0.1),
                layers.RandomContrast(0.2),
            ])
        else:
            return keras.Sequential([
                layers.RandomFlip("horizontal"),
                layers.RandomRotation(0.1),
            ])

    def report_progress(self, progress_data):
        """向SpringBoot报告训练进度"""
        try:
            url = f"{self.springboot_url}/api/training/tasks/{self.task_id}/progress"
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


class ProgressCallback(keras.callbacks.Callback):
    """训练进度回调"""
    def __init__(self, service, batch_size, learning_rate):
        super().__init__()
        self.service = service
        self.batch_size = batch_size
        self.initial_learning_rate = learning_rate
        self.global_step = 0

    def on_train_batch_end(self, batch, logs=None):
        self.global_step += 1

    def on_epoch_end(self, epoch, logs=None):
        if self.service.is_cancelled:
            self.model.stop_training = True
            return
        logs = logs or {}

        # 获取当前学习率
        current_lr = float(keras.backend.get_value(self.model.optimizer.learning_rate))

        # 计算进度
        total_epochs = self.params['epochs']
        progress = (epoch + 1) / total_epochs * 100

        progress_data = {
            "currentEpoch": epoch + 1,
            "progress": progress,
            "loss": float(logs.get("loss", 0.0)),
            "accuracy": float(logs.get("accuracy", 0.0)),
            "valLoss": float(logs.get("val_loss", 0.0)),
            "valAccuracy": float(logs.get("val_accuracy", 0.0)),
            "step": self.global_step,
            "learningRate": current_lr,
            "batchSize": int(self.batch_size),
        }

        self.service.report_progress(progress_data)