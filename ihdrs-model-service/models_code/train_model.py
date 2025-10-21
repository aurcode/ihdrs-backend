# models_code/train_model.py - 训练脚本
#!/usr/bin/env python3
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import tensorflow as tf
from tensorflow import keras
import numpy as np
from pathlib import Path
from config import Config

def train_default_model():
    # 初始化配置
    config = Config()
    config.init_app(None)

    # 1. 加载MNIST数据集
    print("\n加载MNIST数据集...")
    (x_train, y_train), (x_test, y_test) = keras.datasets.mnist.load_data()

    # 2. 数据预处理
    print("数据预处理...")
    x_train = x_train.reshape(-1, 28, 28, 1).astype('float32') / 255.0
    x_test = x_test.reshape(-1, 28, 28, 1).astype('float32') / 255.0

    y_train = keras.utils.to_categorical(y_train, 10)
    y_test = keras.utils.to_categorical(y_test, 10)

    print(f"  训练集形状: {x_train.shape}")
    print(f"  测试集形状: {x_test.shape}")

    # 3. 构建CNN模型
    print("构建CNN模型...")
    model = keras.Sequential([
        keras.layers.Input(shape=(28, 28, 1)),

        # 第一个卷积块
        keras.layers.Conv2D(32, (3, 3), activation='relu', padding='same'),
        keras.layers.BatchNormalization(),
        keras.layers.MaxPooling2D((2, 2)),
        keras.layers.Dropout(0.25),

        # 第二个卷积块
        keras.layers.Conv2D(64, (3, 3), activation='relu', padding='same'),
        keras.layers.BatchNormalization(),
        keras.layers.MaxPooling2D((2, 2)),
        keras.layers.Dropout(0.25),

        # 第三个卷积块
        keras.layers.Conv2D(128, (3, 3), activation='relu', padding='same'),
        keras.layers.BatchNormalization(),
        keras.layers.Dropout(0.25),

        # 全连接层
        keras.layers.Flatten(),
        keras.layers.Dense(128, activation='relu'),
        keras.layers.BatchNormalization(),
        keras.layers.Dropout(0.5),
        keras.layers.Dense(10, activation='softmax')
    ])

    # 编译模型
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=config.DEFAULT_LEARNING_RATE),
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )

    print("\n模型结构:")
    model.summary()

    # 4. 训练
    print(f"\n开始训练 ({config.DEFAULT_EPOCHS}个epoch)...\n")

    callbacks = [
        keras.callbacks.EarlyStopping(
            monitor='val_accuracy',
            patience=config.EARLY_STOPPING_PATIENCE,
            restore_best_weights=True,
            verbose=1
        ),
        keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=2,
            verbose=1
        )
    ]

    history = model.fit(
        x_train, y_train,
        batch_size=config.DEFAULT_BATCH_SIZE,
        epochs=config.DEFAULT_EPOCHS,
        validation_data=(x_test, y_test),
        callbacks=callbacks,
        verbose=1
    )

    # 5. 评估
    print("\n" + "=" * 60)
    test_loss, test_acc = model.evaluate(x_test, y_test, verbose=0)
    print(f"最终测试准确率: {test_acc:.4f} ({test_acc*100:.2f}%)")
    print("=" * 60)

    # 6. 保存模型
    model_path = config.DEFAULT_MODEL_PATH
    model.save(model_path)
    print(f"\n模型已保存到: {model_path}")

    # 7. 快速验证
    print("\n快速验证模型...")
    sample_indices = np.random.choice(len(x_test), 5)
    for idx in sample_indices:
        image = x_test[idx:idx+1]
        true_label = np.argmax(y_test[idx])

        pred = model.predict(image, verbose=0)
        pred_label = np.argmax(pred)
        confidence = pred[0][pred_label]

        result = "✓" if pred_label == true_label else "✗"
        print(f"  {result} 真实值: {true_label}, 预测值: {pred_label}, 置信度: {confidence:.2%}")

    print("\n" + "=" * 60)
    print("=" * 60)

    return model, history

if __name__ == "__main__":
    train_default_model()