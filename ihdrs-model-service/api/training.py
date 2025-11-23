# api/training.py - 训练API
from flask import Blueprint, request, jsonify, current_app
import threading
import time
import requests
from services.training_service import TrainingService
from services.tasks_registry import tasks

training_bp = Blueprint('training', __name__)

@training_bp.route('/train', methods=['POST'])
def start_training():
    """启动训练任务"""
    try:
        data = request.get_json()
        if not data:
            return jsonify({"status": "error", "message": "请求数据为空"}), 400

        task_id = data.get('taskId')
        task_name = data.get('taskName', 'unnamed')
        training_config = data.get('trainingConfig', '{}')
        dataset_config = data.get('datasetConfig', '{}')

        # 创建训练服务实例
        training_service = TrainingService(
            task_id=task_id,
            springboot_url=current_app.config.get('SPRINGBOOT_BASE_URL', 'http://localhost:8080')
        )

        # 异步启动训练
        training_thread = threading.Thread(
            target=training_service.start_training,
            args=(training_config, dataset_config),
            daemon=True
        )
        training_thread.start()

        tasks[task_id] = {
            "thread": training_thread,
            "service": training_service
        }

        return jsonify({
            "status": "success",
            "message": "训练任务已启动",
            "data": {
                "taskId": task_id,
                "taskName": task_name
            }
        })

    except Exception as e:
        current_app.logger.error(f"启动训练任务失败: {e}")
        return jsonify({
            "status": "error",
            "message": "启动训练任务失败",
            "error": str(e)
        }), 500

@training_bp.route('/train/cancel', methods=['POST'])
def cancel_training():
    try:
        data = request.get_json()
        task_id = data.get('taskId')

        if task_id not in tasks:
            return jsonify({"status": "error", "message": "任务不存在"}), 404

        task = tasks[task_id]
        service = task["service"]

        service.is_cancelled = True

        return jsonify({
            "status": "success",
            "message": "训练已标记为取消"
        })

    except Exception as e:
        current_app.logger.error(f"取消训练任务失败: {e}")
        return jsonify({
            "status": "error",
            "message": "取消训练任务失败",
            "error": str(e)
        }), 500
