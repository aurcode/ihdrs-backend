# api/health.py - 健康检查API
from flask import Blueprint, jsonify, current_app
import tensorflow as tf
import os
import psutil
import time

health_bp = Blueprint('health', __name__)

@health_bp.route('/health', methods=['GET'])
def health_check():
    """系统健康检查"""
    try:
        health_status = {
            'status': 'healthy',
            'timestamp': int(time.time()),
            'service': 'ihdrs-model-service',
            'version': '1.0.0'
        }

        # 检查各个组件
        checks = {}

        # 1. TensorFlow状态
        try:
            tf_version = tf.__version__
            checks['tensorflow'] = {
                'status': 'ok',
                'version': tf_version,
                'gpu_available': len(tf.config.list_physical_devices('GPU')) > 0
            }
        except Exception as e:
            checks['tensorflow'] = {
                'status': 'error',
                'error': str(e)
            }

        # 2. 模型服务状态
        try:
            model_service = current_app.model_service
            model_info = model_service.list_models()
            checks['models'] = {
                'status': 'ok',
                'loaded_models': model_info['total_models'],
                'active_model': model_info['active_model_id']
            }
        except Exception as e:
            checks['models'] = {
                'status': 'error',
                'error': str(e)
            }

        # 3. 系统资源
        try:
            memory = psutil.virtual_memory()
            disk = psutil.disk_usage('/')
            checks['system'] = {
                'status': 'ok',
                'memory_usage': f"{memory.percent}%",
                'disk_usage': f"{disk.percent}%",
                'cpu_count': psutil.cpu_count()
            }
        except Exception as e:
            checks['system'] = {
                'status': 'warning',
                'error': str(e)
            }

        # 4. 文件系统检查
        try:
            from config import Config
            model_dir_exists = os.path.exists(Config.MODEL_PATH)
            log_dir_exists = os.path.exists('./logs')

            checks['filesystem'] = {
                'status': 'ok',
                'model_directory': model_dir_exists,
                'log_directory': log_dir_exists
            }
        except Exception as e:
            checks['filesystem'] = {
                'status': 'error',
                'error': str(e)
            }

        # 判断总体健康状态
        error_count = sum(1 for check in checks.values() if check['status'] == 'error')

        if error_count > 0:
            health_status['status'] = 'unhealthy'
        elif any(check['status'] == 'warning' for check in checks.values()):
            health_status['status'] = 'degraded'

        health_status['checks'] = checks

        status_code = 200 if health_status['status'] == 'healthy' else 503

        return jsonify(health_status), status_code

    except Exception as e:
        return jsonify({
            'status': 'unhealthy',
            'timestamp': int(time.time()),
            'error': str(e)
        }), 503

@health_bp.route('/ping', methods=['GET'])
def ping():
    """简单的连接测试"""
    return jsonify({
        'status': 'ok',
        'message': 'pong',
        'timestamp': int(time.time())
    })