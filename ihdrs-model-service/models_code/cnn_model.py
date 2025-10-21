# models_code/cnn_model.py - CNN模型定义
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

def create_cnn_model(input_shape=(28, 28, 1), num_classes=10):
    """
    Args:
        input_shape: 输入图像形状
        num_classes: 分类数量（0-9数字）

    Returns:
        编译好的Keras模型
    """
    model = keras.Sequential([
        # 第一个卷积层
        layers.Conv2D(32, (3, 3), activation='relu', input_shape=input_shape),
        layers.MaxPooling2D((2, 2)),

        # 第二个卷积层
        layers.Conv2D(64, (3, 3), activation='relu'),
        layers.MaxPooling2D((2, 2)),

        # 第三个卷积层
        layers.Conv2D(64, (3, 3), activation='relu'),

        # 展平层
        layers.Flatten(),

        # 全连接层
        layers.Dense(64, activation='relu'),
        layers.Dropout(0.5),

        # 输出层
        layers.Dense(num_classes, activation='softmax')
    ])

    # 编译模型
    model.compile(
        optimizer='adam',
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )

    return model

def create_advanced_cnn_model(input_shape=(28, 28, 1), num_classes=10):
    """
    创建更复杂的CNN模型
    """
    model = keras.Sequential([
        # 第一个卷积块
        layers.Conv2D(32, (3, 3), activation='relu', input_shape=input_shape),
        layers.BatchNormalization(),
        layers.Conv2D(32, (3, 3), activation='relu'),
        layers.MaxPooling2D((2, 2)),
        layers.Dropout(0.25),

        # 第二个卷积块
        layers.Conv2D(64, (3, 3), activation='relu'),
        layers.BatchNormalization(),
        layers.Conv2D(64, (3, 3), activation='relu'),
        layers.MaxPooling2D((2, 2)),
        layers.Dropout(0.25),

        # 全连接层
        layers.Flatten(),
        layers.Dense(512, activation='relu'),
        layers.BatchNormalization(),
        layers.Dropout(0.5),
        layers.Dense(num_classes, activation='softmax')
    ])

    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=0.001),
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )

    return model