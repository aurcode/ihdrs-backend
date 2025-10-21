# tests/becnhmark_performance.py - 性能基准测试
#!/usr/bin/env python3

import requests
import time
import base64
import numpy as np
from PIL import Image
import io
import concurrent.futures
from statistics import mean, stdev

def create_test_image():
    """创建测试图像"""
    image = Image.new('RGB', (28, 28), color='white')
    for i in range(8, 20):
        for j in range(8, 20):
            image.putpixel((i, j), (0, 0, 0))

    buffered = io.BytesIO()
    image.save(buffered, format="PNG")
    return base64.b64encode(buffered.getvalue()).decode()

def single_request(url, image_data):
    """单个请求"""
    start = time.time()
    try:
        response = requests.post(
            url,
            json={"image": image_data, "model_id": 1},
            timeout=10
        )
        elapsed = (time.time() - start) * 1000
        return elapsed if response.status_code == 200 else None
    except:
        return None

def benchmark(url, num_requests=100, concurrency=10):
    """性能基准测试"""
    print(f"开始性能测试: {num_requests}个请求, 并发数: {concurrency}")

    image_data = create_test_image()
    times = []

    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [
            executor.submit(single_request, url, image_data)
            for _ in range(num_requests)
        ]

        for i, future in enumerate(concurrent.futures.as_completed(futures)):
            result = future.result()
            if result:
                times.append(result)
            if (i + 1) % 10 == 0:
                print(f"已完成: {i + 1}/{num_requests}")

    if times:
        print("\n" + "=" * 50)
        print("性能测试结果:")
        print(f"成功请求数: {len(times)}")
        print(f"平均响应时间: {mean(times):.2f}ms")
        print(f"最小响应时间: {min(times):.2f}ms")
        print(f"最大响应时间: {max(times):.2f}ms")
        print(f"标准差: {stdev(times):.2f}ms")
        print(f"QPS: {len(times) / (sum(times) / 1000):.2f}")
        print("=" * 50)

if __name__ == "__main__":
    benchmark("http://localhost:5000/api/recognize", num_requests=50, concurrency=5)