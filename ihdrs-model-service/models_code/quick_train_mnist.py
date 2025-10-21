# models_code/quick_train_mnist.py - 纯CNN模型训练

import tensorflow as tf
from tensorflow import keras
import numpy as np
import sys
from pathlib import Path

# 添加项目根目录到Python路径
BASE_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BASE_DIR))

from config import Config

def train_cnn_model(dataset_name='mnist', epochs=5, batch_size=128):
    """
    训练CNN模型

    Args:
        dataset_name: 数据集名称
        epochs: 训练轮数
        batch_size: 批次大小

    输入(28x28x1)
        ↓
    卷积层 + ReLU
        ↓
    批量归一化 + Dropout
        ↓
    池化层 (MaxPooling)
        ↓
    多层卷积块叠加
        ↓
    Flatten 打平
        ↓
    全连接层 Dense(128)
        ↓
    Softmax 输出10个类别概率

    """
    print("=" * 60)
    print(f"训练CNN模型 (数据集: MINIST)")
    print("=" * 60)

    # 初始化配置
    config = Config()

    # 1. 加载数据集
    print(f"\n加载{dataset_name}数据集...")
    (x_train, y_train), (x_test, y_test) = keras.datasets.mnist.load_data()

    # 2. 数据预处理
    print("数据预处理...")
    # 重塑为 (samples, 28, 28, 1) 并归一化
    x_train = x_train.reshape(-1, 28, 28, 1).astype('float32') / 255.0
    x_test = x_test.reshape(-1, 28, 28, 1).astype('float32') / 255.0

    # One-hot编码
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
    print(f"\n开始训练 ({epochs}个epoch)...\n")

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
        ),
        keras.callbacks.ModelCheckpoint(
            filepath=str(config.MODEL_PATH / 'best_model_checkpoint.h5'),
            monitor='val_accuracy',
            save_best_only=True,
            verbose=1
        )
    ]

    history = model.fit(
        x_train, y_train,
        batch_size=batch_size,
        epochs=epochs,
        validation_data=(x_test, y_test),
        callbacks=callbacks,
        verbose=1
    )

    # 5. 评估
    print("\n" + "=" * 60)
    test_loss, test_acc = model.evaluate(x_test, y_test, verbose=0)
    print(f"最终测试准确率: {test_acc:.4f} ({test_acc*100:.2f}%)")
    print("=" * 60)

    # 6. 保存模型到指定路径
    model_path = config.MODEL_PATH / config.DEFAULT_MODEL_NAME
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
    print("训练完成!")
    print("=" * 60)

    return model, history

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description='训练手写数字识别模型')
    parser.add_argument('--dataset', type=str, default='mnist',
                        choices=['mnist', 'fashion_mnist'],
                        help='数据集名称')
    parser.add_argument('--epochs', type=int, default=5,
                        help='训练轮数')
    parser.add_argument('--batch-size', type=int, default=128,
                        help='批次大小')

    args = parser.parse_args()

    # 检查TensorFlow
    print(f"TensorFlow版本: {tf.__version__}")
    print(f"GPU可用: {len(tf.config.list_physical_devices('GPU')) > 0}\n")

    # 训练模型
    train_cnn_model(
        dataset_name=args.dataset,
        epochs=args.epochs,
        batch_size=args.batch_size
    )