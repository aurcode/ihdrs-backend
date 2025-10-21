# api/training.py - 训练API
from flask import Blueprint, request, jsonify, current_app
import threading
import time
from services.training_service import TrainingService

training_bp = Blueprint('training', __name__)

@training_bp.route('/train', methods=['POST'])
def start_training():
    """
    启动模型训练

    请求格式:
    {
        "task_name": "CNN_Training_v1",
        "epochs": 10,
        "batch_size": 32,
        "learning_rate": 0.001,
        "dataset_config": {...},
        "model_config": {...}
    }
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({
                'status': 'error',
                'message': '请求数据不能为空'
            }), 400

        training_service = TrainingService()

        # 验证训练配置
        validation_error = training_service.validate_training_config(data)
        if validation_error:
            return jsonify({
                'status': 'error',
                'message': validation_error
            }), 400

        # 在后台线程启动训练
        training_thread = threading.Thread(
            target=training_service.start_training,
            args=(data,),
            daemon=True
        )
        training_thread.start()

        return jsonify({
            'status': 'success',
            'message': '训练任务已启动',
            'data': {
                'task_name': data.get('task_name', 'unnamed'),
                'estimated_time': data.get('epochs', 10) * 60  # 估算时间（秒）
            }
        })

    except Exception as e:
        current_app.logger.error(f"启动训练失败: {e}")
        return jsonify({
            'status': 'error',
            'message': '训练服务异常',
            'error': str(e)
        }), 500

@training_bp.route('/train/status', methods=['GET'])
def get_training_status():
    """获取训练状态"""
    try:
        training_service = TrainingService()
        status = training_service.get_training_status()

        return jsonify({
            'status': 'success',
            'data': status
        })

    except Exception as e:
        return jsonify({
            'status': 'error',
            'message': '获取训练状态失败',
            'error': str(e)
        }), 500