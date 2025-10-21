# config.py - 配置文件
import os
from datetime import timedelta
from pathlib import Path

# 项目根目录
BASE_DIR = Path(__file__).resolve().parent

class Config:
    """基础配置类"""
    # Flask基础配置
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'ihdrs-flask-secret-key-2025'

    # 服务配置
    HOST = os.environ.get('FLASK_HOST', '0.0.0.0')
    PORT = int(os.environ.get('FLASK_PORT', 5000))
    DEBUG = os.environ.get('FLASK_DEBUG', 'False').lower() == 'true'

    # 路径配置
    MODEL_PATH = BASE_DIR / 'models'
    DATA_PATH = BASE_DIR / 'data'
    LOG_PATH = BASE_DIR / 'logs'
    UPLOAD_PATH = BASE_DIR / 'uploads'

    # 模型配置
    DEFAULT_MODEL_NAME = 'default_cnn_v1.0.0.h5'
    DEFAULT_MODEL_PATH = MODEL_PATH / DEFAULT_MODEL_NAME

    # 数据集配置
    SUPPORTED_DATASETS = ['mnist', 'fashion_mnist', 'custom']
    DEFAULT_DATASET = 'mnist'
    CUSTOM_DATASET_PATH = DATA_PATH / 'custom'

    # 图像处理配置
    IMAGE_SIZE = (28, 28)
    MAX_IMAGE_SIZE = 5 * 1024 * 1024  # 5MB
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'bmp', 'gif'}

    # 识别配置
    CONFIDENCE_THRESHOLD = 0.8
    MAX_BATCH_SIZE = 32

    # 训练配置
    DEFAULT_EPOCHS = 10
    DEFAULT_BATCH_SIZE = 128
    DEFAULT_LEARNING_RATE = 0.001
    VALIDATION_SPLIT = 0.2
    EARLY_STOPPING_PATIENCE = 3

    # 日志配置
    LOG_LEVEL = os.environ.get('LOG_LEVEL', 'INFO')
    LOG_FILE = 'app.log'
    LOG_MAX_SIZE = 10 * 1024 * 1024  # 10MB
    LOG_BACKUP_COUNT = 5

    @staticmethod
    def init_app(app):
        """初始化应用时创建必要的目录"""
        for path in [Config.MODEL_PATH, Config.DATA_PATH,
                     Config.LOG_PATH, Config.UPLOAD_PATH,
                     Config.CUSTOM_DATASET_PATH]:
            path.mkdir(parents=True, exist_ok=True)

class DevelopmentConfig(Config):
    """开发环境配置"""
    DEBUG = True
    LOG_LEVEL = 'DEBUG'

class ProductionConfig(Config):
    """生产环境配置"""
    DEBUG = False
    LOG_LEVEL = 'WARNING'

class TestingConfig(Config):
    """测试环境配置"""
    TESTING = True
    DEBUG = True
    LOG_LEVEL = 'DEBUG'

config = {
    'development': DevelopmentConfig,
    'production': ProductionConfig,
    'testing': TestingConfig,
    'default': DevelopmentConfig
}