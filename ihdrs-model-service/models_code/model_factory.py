# models_code/model_factory.py - 模型工厂
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers, regularizers

def create_model(model_type, input_shape, num_classes, config):
    """
    模型工厂函数

    Args:
        model_type: 模型类型 (CNN, ADVANCED_CNN, RESNET, VGG, MOBILENET)
        input_shape: 输入形状
        num_classes: 类别数
        config: 配置字典

    Returns:
        编译好的Keras模型
    """
    model_type = model_type.upper()

    if model_type == "CNN":
        model = create_cnn_model(input_shape, num_classes, config)
    elif model_type == "ADVANCED_CNN":
        model = create_advanced_cnn_model(input_shape, num_classes, config)
    elif model_type == "RESNET":
        model = create_resnet_model(input_shape, num_classes, config)
    elif model_type == "VGG":
        model = create_vgg_model(input_shape, num_classes, config)
    elif model_type == "MOBILENET":
        model = create_mobilenet_model(input_shape, num_classes, config)
    else:
        raise ValueError(f"不支持的模型类型: {model_type}")

    return model


def get_activation(activation_name):
    """获取激活函数"""
    activations = {
        'relu': 'relu',
        'leaky_relu': tf.nn.leaky_relu,
        'elu': 'elu',
        'sigmoid': 'sigmoid',
        'tanh': 'tanh'
    }
    return activations.get(activation_name.lower(), 'relu')


def get_regularizer(l2_value):
    """获取L2正则化器"""
    if l2_value > 0:
        return regularizers.l2(l2_value)
    return None


def create_cnn_model(input_shape, num_classes, config):
    """创建基础CNN模型"""
    activation = get_activation(config.get('activation', 'relu'))
    dropout = float(config.get('dropout', 0.2))
    l2_reg = get_regularizer(float(config.get('l2Regularization', 0.0)))
    use_batch_norm = config.get('useBatchNorm', True)

    model = keras.Sequential([
        layers.Input(shape=input_shape),

        layers.Conv2D(32, (3, 3), activation=activation, kernel_regularizer=l2_reg),
        layers.BatchNormalization() if use_batch_norm else layers.Layer(),
        layers.MaxPooling2D((2, 2)),

        layers.Conv2D(64, (3, 3), activation=activation, kernel_regularizer=l2_reg),
        layers.BatchNormalization() if use_batch_norm else layers.Layer(),
        layers.MaxPooling2D((2, 2)),

        layers.Conv2D(64, (3, 3), activation=activation, kernel_regularizer=l2_reg),
        layers.BatchNormalization() if use_batch_norm else layers.Layer(),

        layers.Flatten(),
        layers.Dense(config.get('hiddensize', 128), activation=activation, kernel_regularizer=l2_reg),
        layers.Dropout(dropout),
        layers.Dense(num_classes, activation='softmax')
    ])

    return model


def create_advanced_cnn_model(input_shape, num_classes, config):
    """创建高级CNN模型（带批归一化和更深层次）"""
    activation = get_activation(config.get('activation', 'relu'))
    dropout = float(config.get('dropout', 0.2))
    l2_reg = get_regularizer(float(config.get('l2Regularization', 0.0)))

    model = keras.Sequential([
        layers.Input(shape=input_shape),

        # Block 1
        layers.Conv2D(32, (3, 3), padding='same', kernel_regularizer=l2_reg),
        layers.BatchNormalization(),
        layers.Activation(activation),
        layers.Conv2D(32, (3, 3), padding='same', kernel_regularizer=l2_reg),
        layers.BatchNormalization(),
        layers.Activation(activation),
        layers.MaxPooling2D((2, 2)),
        layers.Dropout(dropout * 0.5),

        # Block 2
        layers.Conv2D(64, (3, 3), padding='same', kernel_regularizer=l2_reg),
        layers.BatchNormalization(),
        layers.Activation(activation),
        layers.Conv2D(64, (3, 3), padding='same', kernel_regularizer=l2_reg),
        layers.BatchNormalization(),
        layers.Activation(activation),
        layers.MaxPooling2D((2, 2)),
        layers.Dropout(dropout * 0.5),

        # Block 3
        layers.Conv2D(128, (3, 3), padding='same', kernel_regularizer=l2_reg),
        layers.BatchNormalization(),
        layers.Activation(activation),
        layers.Conv2D(128, (3, 3), padding='same', kernel_regularizer=l2_reg),
        layers.BatchNormalization(),
        layers.Activation(activation),
        layers.MaxPooling2D((2, 2)),
        layers.Dropout(dropout * 0.5),

        # Fully Connected
        layers.Flatten(),
        layers.Dense(config.get('hiddensize', 256), kernel_regularizer=l2_reg),
        layers.BatchNormalization(),
        layers.Activation(activation),
        layers.Dropout(dropout),
        layers.Dense(num_classes, activation='softmax')
    ])

    return model


def create_resnet_block(x, filters, kernel_size=3, stride=1, activation='relu', l2_reg=None):
    """ResNet残差块"""
    shortcut = x

    # 第一层卷积
    x = layers.Conv2D(filters, kernel_size, strides=stride, padding='same', kernel_regularizer=l2_reg)(x)
    x = layers.BatchNormalization()(x)
    x = layers.Activation(activation)(x)

    # 第二层卷积
    x = layers.Conv2D(filters, kernel_size, padding='same', kernel_regularizer=l2_reg)(x)
    x = layers.BatchNormalization()(x)

    # 如果维度不匹配，使用1x1卷积调整shortcut
    if stride != 1 or shortcut.shape[-1] != filters:
        shortcut = layers.Conv2D(filters, 1, strides=stride, kernel_regularizer=l2_reg)(shortcut)
        shortcut = layers.BatchNormalization()(shortcut)

    # 残差连接
    x = layers.Add()([x, shortcut])
    x = layers.Activation(activation)(x)

    return x


def create_resnet_model(input_shape, num_classes, config):
    """创建ResNet模型"""
    activation = get_activation(config.get('activation', 'relu'))
    dropout = float(config.get('dropout', 0.2))
    l2_reg = get_regularizer(float(config.get('l2Regularization', 0.0)))

    inputs = layers.Input(shape=input_shape)

    # 初始卷积
    x = layers.Conv2D(64, 7, strides=2, padding='same', kernel_regularizer=l2_reg)(inputs)
    x = layers.BatchNormalization()(x)
    x = layers.Activation(activation)(x)
    x = layers.MaxPooling2D(3, strides=2, padding='same')(x)

    # ResNet块
    x = create_resnet_block(x, 64, activation=activation, l2_reg=l2_reg)
    x = create_resnet_block(x, 64, activation=activation, l2_reg=l2_reg)

    x = create_resnet_block(x, 128, stride=2, activation=activation, l2_reg=l2_reg)
    x = create_resnet_block(x, 128, activation=activation, l2_reg=l2_reg)

    x = create_resnet_block(x, 256, stride=2, activation=activation, l2_reg=l2_reg)
    x = create_resnet_block(x, 256, activation=activation, l2_reg=l2_reg)

    # 全局平均池化和分类器
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dense(config.get('hiddensize', 512), activation=activation, kernel_regularizer=l2_reg)(x)
    x = layers.Dropout(dropout)(x)
    outputs = layers.Dense(num_classes, activation='softmax')(x)

    model = keras.Model(inputs=inputs, outputs=outputs, name='ResNet')
    return model


def create_vgg_model(input_shape, num_classes, config):
    """创建VGG风格模型"""
    activation = get_activation(config.get('activation', 'relu'))
    dropout = float(config.get('dropout', 0.5))
    l2_reg = get_regularizer(float(config.get('l2Regularization', 0.0)))

    model = keras.Sequential([
        layers.Input(shape=input_shape),

        # Block 1
        layers.Conv2D(64, (3, 3), activation=activation, padding='same', kernel_regularizer=l2_reg),
        layers.Conv2D(64, (3, 3), activation=activation, padding='same', kernel_regularizer=l2_reg),
        layers.MaxPooling2D((2, 2)),

        # Block 2
        layers.Conv2D(128, (3, 3), activation=activation, padding='same', kernel_regularizer=l2_reg),
        layers.Conv2D(128, (3, 3), activation=activation, padding='same', kernel_regularizer=l2_reg),
        layers.MaxPooling2D((2, 2)),

        # Block 3
        layers.Conv2D(256, (3, 3), activation=activation, padding='same', kernel_regularizer=l2_reg),
        layers.Conv2D(256, (3, 3), activation=activation, padding='same', kernel_regularizer=l2_reg),
        layers.Conv2D(256, (3, 3), activation=activation, padding='same', kernel_regularizer=l2_reg),
        layers.MaxPooling2D((2, 2)),

        # Fully Connected
        layers.Flatten(),
        layers.Dense(config.get('hiddensize', 512), activation=activation, kernel_regularizer=l2_reg),
        layers.Dropout(dropout),
        layers.Dense(config.get('hiddensize', 512), activation=activation, kernel_regularizer=l2_reg),
        layers.Dropout(dropout),
        layers.Dense(num_classes, activation='softmax')
    ], name='VGG')

    return model


def create_mobilenet_model(input_shape, num_classes, config):
    """创建MobileNet风格模型（轻量级）"""
    activation = get_activation(config.get('activation', 'relu'))
    dropout = float(config.get('dropout', 0.2))
    l2_reg = get_regularizer(float(config.get('l2Regularization', 0.0)))

    def depthwise_separable_conv(x, filters, stride=1):
        """深度可分离卷积"""
        x = layers.DepthwiseConv2D(3, strides=stride, padding='same', depthwise_regularizer=l2_reg)(x)
        x = layers.BatchNormalization()(x)
        x = layers.Activation(activation)(x)

        x = layers.Conv2D(filters, 1, padding='same', kernel_regularizer=l2_reg)(x)
        x = layers.BatchNormalization()(x)
        x = layers.Activation(activation)(x)
        return x

    inputs = layers.Input(shape=input_shape)

    # 初始卷积
    x = layers.Conv2D(32, 3, strides=2, padding='same', kernel_regularizer=l2_reg)(inputs)
    x = layers.BatchNormalization()(x)
    x = layers.Activation(activation)(x)

    # 深度可分离卷积块
    x = depthwise_separable_conv(x, 64)
    x = depthwise_separable_conv(x, 128, stride=2)
    x = depthwise_separable_conv(x, 128)
    x = depthwise_separable_conv(x, 256, stride=2)
    x = depthwise_separable_conv(x, 256)
    x = depthwise_separable_conv(x, 512, stride=2)

    # 全局平均池化和分类器
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dense(config.get('hiddensize', 256), activation=activation, kernel_regularizer=l2_reg)(x)
    x = layers.Dropout(dropout)(x)
    outputs = layers.Dense(num_classes, activation='softmax')(x)

    model = keras.Model(inputs=inputs, outputs=outputs, name='MobileNet')
    return model