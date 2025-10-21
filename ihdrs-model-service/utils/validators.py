# utils/validators.py - 数据验证工具
def validate_recognition_request(data):
    """验证识别请求数据"""
    if not isinstance(data, dict):
        return "请求数据必须是JSON对象"

    if 'image' not in data:
        return "缺少必需的image字段"

    image_data = data.get('image')
    if not isinstance(image_data, str):
        return "image字段必须是字符串"

    if len(image_data) == 0:
        return "image数据不能为空"

    # 简单验证Base64格式
    if len(image_data) % 4 != 0:
        return "无效的Base64数据格式"

    model_id = data.get('model_id')
    if model_id is not None and not isinstance(model_id, int):
        return "model_id必须是整数"

    return None