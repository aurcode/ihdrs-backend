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
        model_service.models[model_id] = keras.models.load_model(model_path)

    model_service.active_model_id = model_id
    return jsonify({"status": "success", "active_model_id": model_id})
