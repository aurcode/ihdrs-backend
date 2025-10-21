# tests/check_communication.py - 通信检查脚本
#!/usr/bin/env python3
"""
检查Flask与SpringBoot之间的通信状态
"""
import requests
import json
import time
import base64
from PIL import Image
import io

def create_test_image():
    """创建测试图像"""
    image = Image.new('RGB', (28, 28), color='white')
    # 画一个简单的图案
    for i in range(8, 20):
        for j in range(8, 20):
            image.putpixel((i, j), (0, 0, 0))

    buffered = io.BytesIO()
    image.save(buffered, format="PNG")
    return base64.b64encode(buffered.getvalue()).decode()

def check_flask_service():
    """检查Flask服务"""
    try:
        print("🔍 检查Flask服务...")

        # 健康检查
        response = requests.get("http://localhost:5000/health", timeout=5)
        if response.status_code == 200:
            print("✅ Flask健康检查成功")
            data = response.json()
            print(f"   状态: {data.get('status')}")
        else:
            print(f"❌ Flask健康检查失败: {response.status_code}")
            return False

        # 识别测试
        image_data = create_test_image()
        response = requests.post(
            "http://localhost:5000/api/recognize",
            json={"image": image_data, "model_id": 1},
            timeout=10
        )

        if response.status_code == 200:
            print("✅ Flask识别测试成功")
            data = response.json()
            if data.get('status') == 'success':
                result = data.get('data', {})
                print(f"   识别结果: {result.get('result')}")
                print(f"   置信度: {result.get('confidence', 0):.4f}")
                print(f"   处理时间: {result.get('processing_time')}ms")
            else:
                print(f"   识别失败: {data.get('message')}")
        else:
            print(f"❌ Flask识别测试失败: {response.status_code}")
            print(f"   响应: {response.text}")
            return False

        return True

    except Exception as e:
        print(f"❌ Flask服务连接失败: {e}")
        return False

def check_springboot_service():
    """检查SpringBoot服务"""
    try:
        print("\n🔍 检查SpringBoot服务...")

        # 健康检查
        response = requests.get("http://localhost:8080/api/health", timeout=5)
        if response.status_code == 200:
            print("✅ SpringBoot健康检查成功")
        else:
            print(f"❌ SpringBoot健康检查失败: {response.status_code}")
            return False

        # 测试数据库连接
        response = requests.get("http://localhost:8080/api/test/db", timeout=5)
        if response.status_code == 200:
            print("✅ SpringBoot数据库连接成功")
            data = response.json()
            if data.get('code') == 200:
                db_info = data.get('data', {})
                print(f"   数据库状态: {db_info.get('database')}")
                print(f"   用户数量: {db_info.get('userCount')}")

        return True

    except Exception as e:
        print(f"❌ SpringBoot服务连接失败: {e}")
        return False

def check_integration():
    """检查集成通信"""
    try:
        print("\n🔍 检查集成通信...")

        # 通过SpringBoot调用识别服务
        image_data = create_test_image()
        response = requests.post(
            "http://localhost:8080/api/recognition/recognize",
            json={
                "imageData": image_data,
                "inputType": "CANVAS",
                "sessionId": "test_session"
            },
            timeout=15
        )

        if response.status_code == 200:
            print("✅ SpringBoot->Flask集成通信成功")
            data = response.json()
            if data.get('code') == 200:
                result = data.get('data', {})
                print(f"   识别结果: {result.get('recognitionResult')}")
                print(f"   置信度: {result.get('confidence', 0):.4f}")
                print(f"   处理时间: {result.get('processingTime')}ms")
                return True
            else:
                print(f"   集成调用失败: {data.get('message')}")
        else:
            print(f"❌ SpringBoot->Flask集成通信失败: {response.status_code}")
            print(f"   响应: {response.text}")

        return False

    except Exception as e:
        print(f"❌ 集成通信测试失败: {e}")
        return False

def main():
    """主函数"""
    print("=" * 60)
    print("手写数字识别系统 - 通信检查工具")
    print("=" * 60)

    # 检查各个服务
    flask_ok = check_flask_service()
    springboot_ok = check_springboot_service()

    if flask_ok and springboot_ok:
        integration_ok = check_integration()
    else:
        integration_ok = False

    # 总结
    print("\n" + "=" * 60)
    print("检查结果总结:")
    print(f"Flask服务: {'✅ 正常' if flask_ok else '❌ 异常'}")
    print(f"SpringBoot服务: {'✅ 正常' if springboot_ok else '❌ 异常'}")
    print(f"集成通信: {'✅ 正常' if integration_ok else '❌ 异常'}")

    if flask_ok and springboot_ok and integration_ok:
        print("\n🎉 所有服务运行正常，系统就绪！")
        return 0
    else:
        print("\n⚠️  部分服务存在问题，请检查:")
        if not flask_ok:
            print("   - Flask服务未正常运行 (端口5000)")
        if not springboot_ok:
            print("   - SpringBoot服务未正常运行 (端口8080)")
        if not integration_ok:
            print("   - 服务间通信存在问题")
        return 1

if __name__ == "__main__":
    exit(main())