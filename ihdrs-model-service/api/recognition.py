# api/recognition.py - 识别API
from flask import Blueprint, request, jsonify, current_app
import base64
import numpy as np
import time
from services.image_processor import ImageProcessor
from utils.validators import validate_recognition_request

recognition_bp = Blueprint('recognition', __name__)

@recognition_bp.route('/recognize', methods=['POST'])
def recognize():
    """手写数字识别接口"""
    start_time = time.time()

    try:
        if not request.is_json:
            return jsonify({
                'status': 'error',
                'message': '请求Content-Type必须为application/json'
            }), 400

        # 获取请求数据
        data = request.get_json(silent=True)  # 使用silent=True避免抛出异常
        if not data:
            return jsonify({
                'status': 'error',
                'message': '请求数据不能为空'
            }), 400

        # 验证请求参数
        validation_error = validate_recognition_request(data)
        if validation_error:
            return jsonify({
                'status': 'error',
                'message': validation_error
            }), 400

        image_data = data.get('image')
        model_id = data.get('model_id', 1)

        # 解码Base64图像
        try:
            image_bytes = base64.b64decode(image_data)
        except Exception as e:
            current_app.logger.error(f"Base64解码失败: {e}")
            return jsonify({
                'status': 'error',
                'message': 'Base64图像数据格式错误'
            }), 400

        # 图像预处理
        image_processor = ImageProcessor()
        processed_image = image_processor.preprocess_for_recognition(image_bytes)

        if processed_image is None:
            return jsonify({
                'status': 'error',
                'message': '图像处理失败'
            }), 400

        # 执行识别
        model_service = current_app.model_service
        prediction_result = model_service.predict(processed_image, model_id)

        if prediction_result is None:
            return jsonify({
                'status': 'error',
                'message': '模型预测失败'
            }), 500

        processing_time = int((time.time() - start_time) * 1000)

        # 构建响应
        response_data = {
            'result': int(prediction_result['digit']),
            'confidence': float(prediction_result['confidence']),
            'processing_time': processing_time,
            'all_probabilities': prediction_result.get('all_probabilities', [])
        }

        current_app.logger.info(f"识别完成 - 结果: {response_data['result']}, "
                                f"置信度: {response_data['confidence']:.4f}, "
                                f"耗时: {processing_time}ms")

        return jsonify({
            'status': 'success',
            'data': response_data
        })

    except Exception as e:
        current_app.logger.error(f"识别过程发生错误: {e}", exc_info=True)
        return jsonify({
            'status': 'error',
            'message': '识别服务异常',
            'error': str(e)
        }), 500

@recognition_bp.route('/recognize_multi', methods=['POST'])
def recognize_multi():
    start = time.time()

    data = request.get_json()
    image_base64 = data.get("image")
    model_id = data.get("model_id", 1)
    debug = data.get("debug", False)  # 新增调试模式参数

    # 1. 解码
    image_bytes = base64.b64decode(image_base64)

    # 2. 分割多个数字
    processor = ImageProcessor()
    digits = processor.segment_digits(image_bytes, debug=debug)  # 传入debug参数
    current_app.logger.info(f"分割数量: {len(digits)}")
    if not digits:
        return jsonify({"status": "error", "message": "无法分割数字"}), 400

    model_service = current_app.model_service

    results = []
    for digit_img in digits:
        pred = model_service.predict(digit_img, model_id)
        results.append({
            "digit": int(pred['digit']),
            "confidence": float(pred['confidence']),
            "all_probabilities": pred.get('all_probabilities', [])
        })

    response_data = {
        "status": "success",
        "data": {
            "count": len(results),
            "results": results,
            "processing_time": int((time.time() - start) * 1000)
        }
    }

    # 如果启用了调试模式，添加额外的调试信息
    if debug:
        response_data["debug"] = {
            "segmentation_enabled": True,
            "optimization_version": "2.0",
            "features": [
                "adaptive_thresholding",
                "stroke_width_estimation", 
                "connectivity_analysis",
                "contour_merging",
                "dynamic_morphology"
            ]
        }

    return jsonify(response_data)
