# api/model.py
from flask import Blueprint, request, jsonify, current_app
from tensorflow import keras

model_bp = Blueprint('model', __name__)

@model_bp.route('/models/activate', methods=['POST'])
def activate_model():
    data = request.get_json()
    model_id = data.get("model_id")
    model_path = data.get("model_path")
    model_service = current_app.model_service

    # 如果模型未加载，则加载
    if model_id not in model_service.models:
        current_app.logger.info(f"加载模型: id={model_id}, path={model_path}")
        model = keras.models.load_model(model_path)
        model_service.models[model_id] = model

        # 检测模型输入类型
        input_shape = model.input_shape
        if len(input_shape) == 2 and input_shape[1] == 784:
            model_service.model_input_type[model_id] = 'flatten'
        elif len(input_shape) == 4:
            model_service.model_input_type[model_id] = 'cnn'
        else:
            model_service.model_input_type[model_id] = 'unknown'

        # 保存模型路径
        model_service.model_paths[model_id] = model_path

        # 保存元数据
        model_service.model_metadata[model_id] = {
            'path': model_path,
            'input_type': model_service.model_input_type[model_id],
            'input_shape': input_shape
        }

        # 预热模型
        model_service._warmup_model(model, model_service.model_input_type[model_id])
        current_app.logger.info(f"模型加载并预热完成: id={model_id}")

    model_service.active_model_id = model_id

    return jsonify({
        "status": "success",
        "active_model_id": model_id,
        "message": "模型已激活并预热"
    })