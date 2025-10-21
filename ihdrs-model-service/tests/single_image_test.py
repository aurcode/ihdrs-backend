import base64, requests

with open('test_digit.png', 'rb') as f:
    image_data = base64.b64encode(f.read()).decode()

    response = requests.post(
        'http://localhost:5000/api/recognize',
        json={
            'image': image_data,
            'model_id': 1
        }
    )

print(response.json())