# app.py - 主应用文件
import os
import logging
from flask import Flask, jsonify
from flask_cors import CORS
from config import config
from utils.logger import setup_logger
from services.model_service import ModelService

def create_app(config_name=None):
    """应用工厂函数"""
    if config_name is None:
        config_name = os.environ.get('FLASK_CONFIG') or 'development'

    app = Flask(__name__)
    app.config.from_object(config[config_name])

    # 初始化配置
    config[config_name].init_app(app)

    # 配置CORS
    CORS(app, origins="*", supports_credentials=True)

    # 设置日志
    setup_logger(app)

    # 在这里初始化模型服务（只初始化一次）
    model_service = ModelService()

    # 立即加载模型（而不是在first_request时）
    try:
        if model_service.load_default_model():
            app.logger.info("默认模型加载并预热成功")
        else:
            app.logger.warning("默认模型加载失败，将在首次训练时创建")
    except Exception as e:
        app.logger.error(f"模型服务初始化失败: {e}")

    # 将模型服务附加到app
    app.model_service = model_service

    # 注册蓝图
    from api.recognition import recognition_bp
    from api.training import training_bp
    from api.health import health_bp

    app.register_blueprint(recognition_bp, url_prefix='/api')
    app.register_blueprint(training_bp, url_prefix='/api')
    app.register_blueprint(health_bp)

    # 全局错误处理
    @app.errorhandler(400)
    def bad_request(error):
        return jsonify({
            'status': 'error',
            'message': '请求参数错误',
            'error': str(error)
        }), 400

    @app.errorhandler(404)
    def not_found(error):
        return jsonify({
            'status': 'error',
            'message': '接口不存在',
            'error': str(error)
        }), 404

    @app.errorhandler(500)
    def internal_error(error):
        return jsonify({
            'status': 'error',
            'message': '服务器内部错误',
            'error': str(error)
        }), 500

    return app