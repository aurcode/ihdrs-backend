#!/usr/bin/env python3
"""
测试脚本 - 验证连续数字分割优化效果
用于测试和验证图像处理器中新的分割算法
"""

import base64
import time
import requests
import json
import logging
from pathlib import Path
import sys

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# API配置
API_BASE_URL = "http://localhost:5000/api"
TEST_IMAGES_DIR = Path(__file__).parent / "test_images"
RESULTS_DIR = Path(__file__).parent / "test_results"

def create_test_images():
    """创建测试用的连续数字图像（如果不存在）"""
    TEST_IMAGES_DIR.mkdir(exist_ok=True)
    RESULTS_DIR.mkdir(exist_ok=True)
    
    # 这里可以添加创建测试图像的逻辑
    # 或者使用现有的测试图像
    test_images = list(TEST_IMAGES_DIR.glob("*.png")) + list(TEST_IMAGES_DIR.glob("*.jpg"))
    
    if not test_images:
        logger.warning("未找到测试图像，请在 tests/test_images/ 目录中添加测试图像")
        return []
    
    return test_images

def test_single_digit_recognition(image_path):
    """测试单个数字识别"""
    logger.info(f"测试单个数字识别: {image_path.name}")
    
    try:
        with open(image_path, 'rb') as f:
            image_data = base64.b64encode(f.read()).decode()
        
        # 测试标准模式
        response = requests.post(
            f"{API_BASE_URL}/recognize",
            json={'image': image_data, 'model_id': 1}
        )
        
        if response.status_code == 200:
            result = response.json()
            logger.info(f"标准模式 - 结果: {result}")
            return result
        else:
            logger.error(f"标准模式失败: {response.status_code} - {response.text}")
            return None
            
    except Exception as e:
        logger.error(f"单个数字测试失败: {e}")
        return None

def test_multi_digit_segmentation(image_path, debug=False):
    """测试连续数字分割"""
    logger.info(f"测试连续数字分割: {image_path.name} (debug={debug})")
    
    try:
        with open(image_path, 'rb') as f:
            image_data = base64.b64encode(f.read()).decode()
        
        start_time = time.time()
        
        # 测试多数字识别
        response = requests.post(
            f"{API_BASE_URL}/recognize_multi",
            json={'image': image_data, 'model_id': 1, 'debug': debug}
        )
        
        processing_time = time.time() - start_time
        
        if response.status_code == 200:
            result = response.json()
            
            # 记录结果
            result['processing_time_external'] = round(processing_time, 3)
            result['image_name'] = image_path.name
            result['debug_mode'] = debug
            
            logger.info(f"分割结果: 找到 {result['data']['count']} 个数字")
            logger.info(f"识别结果: {[r['digit'] for r in result['data']['results']]}")
            logger.info(f"置信度: {[round(r['confidence'], 3) for r in result['data']['results']]}")
            logger.info(f"总耗时: {processing_time:.3f}s")
            
            if debug and 'debug' in result:
                logger.info(f"调试信息: {result['debug']}")
            
            return result
        else:
            logger.error(f"分割失败: {response.status_code} - {response.text}")
            return None
            
    except Exception as e:
        logger.error(f"连续数字测试失败: {e}")
        return None

def compare_optimizations(image_path):
    """比较优化前后的效果"""
    logger.info(f"\n=== 比较优化效果: {image_path.name} ===")
    
    # 测试标准模式（debug=False）
    result_normal = test_multi_digit_segmentation(image_path, debug=False)
    
    # 等待一下避免API过载
    time.sleep(0.5)
    
    # 测试调试模式（debug=True）
    result_debug = test_multi_digit_segmentation(image_path, debug=True)
    
    if result_normal and result_debug:
        # 比较结果
        count_normal = result_normal['data']['count']
        count_debug = result_debug['data']['count']
        time_normal = result_normal['processing_time_external']
        time_debug = result_debug['processing_time_external']
        
        logger.info(f"\n比较结果:")
        logger.info(f"标准模式 - 分割数量: {count_normal}, 耗时: {time_normal:.3f}s")
        logger.info(f"调试模式 - 分割数量: {count_debug}, 耗时: {time_debug:.3f}s")
        
        if count_debug > count_normal:
            logger.info("✅ 调试模式分割效果更好")
        elif count_debug == count_normal:
            logger.info("⚖️ 两种模式分割效果相同")
        else:
            logger.info("❌ 标准模式分割效果更好")
        
        return {
            'image': image_path.name,
            'normal_count': count_normal,
            'debug_count': count_debug,
            'normal_time': time_normal,
            'debug_time': time_debug
        }
    
    return None

def run_comprehensive_tests():
    """运行综合测试"""
    logger.info("开始连续数字分割优化测试...")
    
    # 创建测试图像列表
    test_images = create_test_images()
    
    if not test_images:
        logger.error("没有找到测试图像，测试终止")
        return
    
    logger.info(f"找到 {len(test_images)} 个测试图像")
    
    # 测试结果汇总
    all_results = []
    comparison_results = []
    
    for image_path in test_images:
        try:
            # 测试单个数字识别
            single_result = test_single_digit_recognition(image_path)
            
            # 测试连续数字分割（标准模式）
            multi_result_normal = test_multi_digit_segmentation(image_path, debug=False)
            
            # 测试连续数字分割（调试模式）
            multi_result_debug = test_multi_digit_segmentation(image_path, debug=True)
            
            # 比较优化效果
            comparison = compare_optimizations(image_path)
            if comparison:
                comparison_results.append(comparison)
            
            # 等待一下避免API过载
            time.sleep(1)
            
        except Exception as e:
            logger.error(f"测试 {image_path.name} 时出错: {e}")
            continue
    
    # 生成测试报告
    generate_test_report(comparison_results)

def generate_test_report(comparison_results):
    """生成测试报告"""
    if not comparison_results:
        logger.warning("没有比较结果可生成报告")
        return
    
    report_file = RESULTS_DIR / "segmentation_test_report.json"
    
    # 统计信息
    total_tests = len(comparison_results)
    improved_cases = sum(1 for r in comparison_results if r['debug_count'] > r['normal_count'])
    same_cases = sum(1 for r in comparison_results if r['debug_count'] == r['normal_count'])
    worse_cases = sum(1 for r in comparison_results if r['debug_count'] < r['normal_count'])
    
    avg_normal_time = sum(r['normal_time'] for r in comparison_results) / total_tests
    avg_debug_time = sum(r['debug_time'] for r in comparison_results) / total_tests
    
    report = {
        'summary': {
            'total_tests': total_tests,
            'improved_cases': improved_cases,
            'same_cases': same_cases,
            'worse_cases': worse_cases,
            'improvement_rate': round(improved_cases / total_tests * 100, 1) if total_tests > 0 else 0,
            'avg_normal_time': round(avg_normal_time, 3),
            'avg_debug_time': round(avg_debug_time, 3),
            'time_overhead': round((avg_debug_time - avg_normal_time) / avg_normal_time * 100, 1) if avg_normal_time > 0 else 0
        },
        'detailed_results': comparison_results,
        'timestamp': time.strftime('%Y-%m-%d %H:%M:%S')
    }
    
    # 保存报告
    with open(report_file, 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    # 打印摘要
    logger.info(f"\n=== 测试报告摘要 ===")
    logger.info(f"总测试数: {total_tests}")
    logger.info(f"改进案例: {improved_cases} ({report['summary']['improvement_rate']}%)")
    logger.info(f"相同效果: {same_cases}")
    logger.info(f"效果变差: {worse_cases}")
    logger.info(f"平均耗时 - 标准模式: {avg_normal_time:.3f}s, 调试模式: {avg_debug_time:.3f}s")
    logger.info(f"时间开销增加: {report['summary']['time_overhead']}%")
    logger.info(f"详细报告已保存: {report_file}")

def main():
    """主函数"""
    if len(sys.argv) > 1:
        # 测试指定图像
        image_path = Path(sys.argv[1])
        if image_path.exists():
            test_multi_digit_segmentation(image_path, debug=True)
        else:
            logger.error(f"图像文件不存在: {image_path}")
    else:
        # 运行完整测试
        run_comprehensive_tests()

if __name__ == "__main__":
    main()
