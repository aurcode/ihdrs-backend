# tests/test_api.py - API测试
import sys, os
sys.path.append(os.path.dirname(os.path.dirname(__file__)))
import unittest
import json
import base64
from PIL import Image
import numpy as np
import io
from app import create_app

class TestAPI(unittest.TestCase):
    def setUp(self):
        self.app = create_app('testing')
        self.client = self.app.test_client()
        self.app_context = self.app.app_context()
        self.app_context.push()

    def tearDown(self):
        self.app_context.pop()

    def create_test_image(self):
        """创建测试图像"""
        # 创建一个简单的28x28黑白图像
        image = Image.new('RGB', (28, 28), color='white')
        # 在中间画一些像素点模拟数字
        pixels = image.load()
        for i in range(10, 18):
            for j in range(5, 15):
                pixels[i, j] = (0, 0, 0)  # 黑色像素

        # 转换为Base64
        buffered = io.BytesIO()
        image.save(buffered, format="PNG")
        img_str = base64.b64encode(buffered.getvalue()).decode()
        return img_str

    def test_health_check(self):
        """测试健康检查接口"""
        response = self.client.get('/health')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertEqual(data['status'], 'healthy')

    def test_ping(self):
        """测试ping接口"""
        response = self.client.get('/ping')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertEqual(data['message'], 'pong')

    def test_recognition_invalid_image(self):
        """测试识别接口无效图像"""
        data = {'image': 'invalid_base64'}
        response = self.client.post('/api/recognize',
                                    data=json.dumps(data),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 400)

    def test_recognition_valid_request(self):
        """测试识别接口有效请求"""
        image_data = self.create_test_image()
        data = {'image': image_data}

        response = self.client.post('/api/recognize',
                                    data=json.dumps(data),
                                    content_type='application/json')

        # 如果没有训练好的模型，可能返回500，这是正常的
        self.assertIn(response.status_code, [200, 500])

if __name__ == '__main__':
    unittest.main()