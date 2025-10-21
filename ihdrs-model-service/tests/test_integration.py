# tests/test_integration.py - 集成测试
import unittest
import requests
import json
import base64
import time
import numpy as np
from PIL import Image, ImageDraw
import io


class TestFlaskSpringBootIntegration(unittest.TestCase):
    """Flask与SpringBoot集成测试"""

    def setUp(self):
        self.flask_url = "http://localhost:5000"
        self.springboot_url = "http://localhost:8080/api"
        self.timeout = 30

    def create_digit_image(self, digit=5):
        """创建模拟手写数字图像"""
        image = Image.new('RGB', (280, 280), color='white')
        draw = ImageDraw.Draw(image)

        # 简单画一个数字5的轮廓
        if digit == 5:
            # 画数字5
            draw.rectangle([50, 50, 200, 80], fill='black')  # 横线
            draw.rectangle([50, 50, 80, 150], fill='black')  # 竖线
            draw.rectangle([50, 150, 200, 180], fill='black')  # 中间横线
            draw.rectangle([170, 150, 200, 250], fill='black')  # 右竖线
            draw.rectangle([50, 220, 200, 250], fill='black')  # 底横线
        else:
            # 简单画个圆表示其他数字
            draw.ellipse([80, 80, 200, 200], fill='black')

        # 转换为28x28灰度图
        image = image.resize((28, 28)).convert('L')

        # 转换为Base64
        buffered = io.BytesIO()
        image.save(buffered, format="PNG")
        img_str = base64.b64encode(buffered.getvalue()).decode()
        return img_str

    def test_flask_health(self):
        """测试Flask健康检查"""
        try:
            response = requests.get(f"{self.flask_url}/health", timeout=self.timeout)
            self.assertEqual(response.status_code, 200)
            data = response.json()
            print(f"Flask健康状态: {data['status']}")
            return True
        except Exception as e:
            print(f"Flask健康检查失败: {e}")
            return False

    def test_springboot_health(self):
        """测试SpringBoot健康检查"""
        try:
            response = requests.get(f"{self.springboot_url}/health", timeout=self.timeout)
            self.assertEqual(response.status_code, 200)
            data = response.json()
            print(f"SpringBoot健康状态: {data.get('message', 'OK')}")
            return True
        except Exception as e:
            print(f"SpringBoot健康检查失败: {e}")
            return False

    def test_flask_recognition_direct(self):
        """直接测试Flask识别接口"""
        image_data = self.create_digit_image(5)

        payload = {
            "image": image_data,
            "model_id": 1
        }

        try:
            response = requests.post(
                f"{self.flask_url}/api/recognize",
                json=payload,
                timeout=self.timeout
            )

            print(f"Flask识别响应状态: {response.status_code}")
            if response.status_code == 200:
                data = response.json()
                print(f"Flask识别结果: {data}")
                self.assertEqual(data['status'], 'success')
                self.assertIn('result', data['data'])
                return True
            else:
                print(f"Flask识别失败: {response.text}")
                return False
        except Exception as e:
            print(f"Flask识别测试异常: {e}")
            return False

    def test_springboot_recognition_proxy(self):
        """测试SpringBoot代理的识别接口"""
        image_data = self.create_digit_image(5)

        payload = {
            "imageData": image_data,
            "inputType": "CANVAS"
        }

        try:
            response = requests.post(
                f"{self.springboot_url}/recognition/recognize",
                json=payload,
                timeout=self.timeout
            )

            print(f"SpringBoot识别响应状态: {response.status_code}")
            if response.status_code == 200:
                data = response.json()
                print(f"SpringBoot识别结果: {data}")
                self.assertEqual(data['code'], 200)
                self.assertIn('recognitionResult', data['data'])
                return True
            else:
                print(f"SpringBoot识别失败: {response.text}")
                return False
        except Exception as e:
            print(f"SpringBoot识别测试异常: {e}")
            return False

    def test_full_integration(self):
        """完整集成测试"""
        print("\n" + "=" * 50)
        print("开始完整集成测试")
        print("=" * 50)

        # 1. 检查服务健康状态
        print("\n1. 检查服务健康状态...")
        flask_healthy = self.test_flask_health()
        springboot_healthy = self.test_springboot_health()

        if not flask_healthy:
            self.fail("Flask服务不健康")
        if not springboot_healthy:
            self.fail("SpringBoot服务不健康")

        # 2. 测试Flask直接识别
        print("\n2. 测试Flask直接识别...")
        flask_recognition = self.test_flask_recognition_direct()

        # 3. 测试SpringBoot代理识别
        print("\n3. 测试SpringBoot代理识别...")
        springboot_recognition = self.test_springboot_recognition_proxy()

        print("\n" + "=" * 50)
        print("集成测试结果:")
        print(f"Flask健康检查: {'✓' if flask_healthy else '✗'}")
        print(f"SpringBoot健康检查: {'✓' if springboot_healthy else '✗'}")
        print(f"Flask直接识别: {'✓' if flask_recognition else '✗'}")
        print(f"SpringBoot代理识别: {'✓' if springboot_recognition else '✗'}")
        print("=" * 50)


class TestPerformance(unittest.TestCase):
    """性能测试"""

    def setUp(self):
        self.flask_url = "http://localhost:5000"
        self.springboot_url = "http://localhost:8080/api"

    def create_test_image(self):
        """创建测试图像"""
        image = Image.new('RGB', (28, 28), color='white')
        draw = ImageDraw.Draw(image)
        draw.rectangle([5, 5, 23, 23], fill='black')

        buffered = io.BytesIO()
        image.save(buffered, format="PNG")
        return base64.b64encode(buffered.getvalue()).decode()

    def test_recognition_performance(self):
        """测试识别性能"""
        image_data = self.create_test_image()
        times = []

        print("\n性能测试 - 10次识别请求:")
        for i in range(10):
            start_time = time.time()

            try:
                response = requests.post(
                    f"{self.flask_url}/api/recognize",
                    json={"image": image_data, "model_id": 1},
                    timeout=10
                )

                end_time = time.time()
                response_time = (end_time - start_time) * 1000  # 毫秒
                times.append(response_time)

                print(f"请求 {i + 1}: {response_time:.2f}ms - 状态: {response.status_code}")

            except Exception as e:
                print(f"请求 {i + 1}: 失败 - {e}")

        if times:
            avg_time = np.mean(times)
            min_time = np.min(times)
            max_time = np.max(times)

            print(f"\n性能统计:")
            print(f"平均响应时间: {avg_time:.2f}ms")
            print(f"最小响应时间: {min_time:.2f}ms")
            print(f"最大响应时间: {max_time:.2f}ms")

            # 性能要求：平均响应时间应小于3秒
            self.assertLess(avg_time, 3000, "平均响应时间超过3秒")


if __name__ == '__main__':
    # 创建测试套件
    test_suite = unittest.TestSuite()

    # 添加集成测试
    test_suite.addTest(TestFlaskSpringBootIntegration('test_full_integration'))

    # 添加性能测试
    test_suite.addTest(TestPerformance('test_recognition_performance'))

    # 运行测试
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(test_suite)
