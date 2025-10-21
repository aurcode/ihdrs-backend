# utils/logger.py - 日志配置
import logging
import os
from logging.handlers import RotatingFileHandler

def setup_logger(app):
    """设置应用日志"""
    if not app.debug:
        # 确保日志目录存在
        os.makedirs('./logs', exist_ok=True)

        # 设置文件日志处理器
        file_handler = RotatingFileHandler(
            './logs/app.log',
            maxBytes=app.config.get('LOG_MAX_SIZE', 10240000),
            backupCount=app.config.get('LOG_BACKUP_COUNT', 10)
        )

        file_handler.setFormatter(logging.Formatter(
            '%(asctime)s %(levelname)s: %(message)s [in %(pathname)s:%(lineno)d]'
        ))

        file_handler.setLevel(logging.INFO)
        app.logger.addHandler(file_handler)
        app.logger.setLevel(logging.INFO)
        app.logger.info('Flask模型服务启动')