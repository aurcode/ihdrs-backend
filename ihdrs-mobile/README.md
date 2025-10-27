# IHDRS Mobile App

A React Native mobile application for handwritten digit recognition using the IHDRS (Intelligent Handwritten Digit Recognition System) backend.

## Features

- ✏️ **Draw Mode**: Draw digits directly on a canvas
- 📤 **Upload Mode**: Upload images from gallery or take photos
- 🤖 **AI Recognition**: Real-time digit recognition using TensorFlow model
- 📊 **Detailed Results**: View predicted digit, confidence score, and probability distribution

## Prerequisites

- Node.js (v14 or higher)
- npm or yarn
- Expo CLI (`npm install -g expo-cli`)
- iOS Simulator (for iOS development) or Android Studio (for Android development)
- IHDRS Backend running (Java Spring Boot + Python Flask services)

## Installation

1. Navigate to the mobile directory:
```bash
cd ihdrs-mobile
```

2. Install dependencies:
```bash
npm install
```

## Configuration

Before running the app, configure the API endpoints in `src/config/api.js`:

```javascript
export const API_CONFIG = {
  BACKEND_URL: 'http://YOUR_BACKEND_IP:8080',  // Update with your backend URL
  MODEL_SERVICE_URL: 'http://YOUR_MODEL_IP:5000',
  // ...
};
```

### Important Notes:
- For iOS Simulator: Use `http://localhost:8080` if backend is on the same machine
- For Android Emulator: Use `http://10.0.2.2:8080` to access localhost
- For Physical Devices: Use your computer's IP address (e.g., `http://192.168.1.100:8080`)

## Running the App

1. Start the Expo development server:
```bash
npm start
```

2. Choose your platform:
   - Press `i` for iOS Simulator
   - Press `a` for Android Emulator
   - Scan QR code with Expo Go app on your physical device

## Project Structure

```
ihdrs-mobile/
├── App.js                          # Main application component
├── src/
│   ├── components/
│   │   ├── DrawingCanvas.js        # Canvas component for drawing digits
│   │   └── ImagePickerComponent.js # Image picker/camera component
│   ├── services/
│   │   └── recognitionService.js   # API service for digit recognition
│   └── config/
│       └── api.js                  # API configuration
├── assets/                         # App assets (icons, images)
├── package.json
└── README.md
```

## Usage

### Draw Mode
1. Select "Draw" mode
2. Draw a digit (0-9) on the canvas
3. Tap "Recognize" to send to AI model
4. View the prediction results

### Upload Mode
1. Select "Upload" mode
2. Choose "Pick from Gallery" or "Take Photo"
3. Select/capture an image of a digit
4. The app automatically sends it for recognition
5. View the prediction results

## API Integration

The app communicates with the IHDRS backend through the following endpoint:

- **POST** `/recognition/recognize`
  - Request body: `{ "imageData": "base64_encoded_image" }`
  - Response: Predicted digit, confidence score, and probability distribution

## Dependencies

- **expo**: ~54.0.20 - Expo framework
- **react-native**: 0.81.5 - React Native framework
- **expo-image-picker**: ~16.0.4 - Image picker and camera functionality
- **react-native-svg**: 15.9.0 - SVG support for drawing canvas
- **axios**: ^1.6.0 - HTTP client for API requests

## Troubleshooting

### Cannot connect to backend
- Ensure the backend services are running
- Check the API URL configuration in `src/config/api.js`
- For Android emulator, use `10.0.2.2` instead of `localhost`
- For physical devices, ensure they're on the same network as the backend

### Image picker not working
- Grant camera and photo library permissions when prompted
- Check app permissions in device settings

### Drawing canvas not responding
- Ensure you're touching within the canvas boundaries
- Try clearing the canvas and drawing again

## Development

To modify the app:

1. **Add new features**: Create components in `src/components/`
2. **Modify API calls**: Update `src/services/recognitionService.js`
3. **Change styling**: Update styles in respective component files
4. **Configure endpoints**: Modify `src/config/api.js`

## Building for Production

### iOS
```bash
expo build:ios
```

### Android
```bash
expo build:android
```

## Known Limitations

- Drawing mode currently shows a placeholder for recognition (canvas-to-image conversion needs implementation)
- Upload mode is fully functional with image recognition
- Requires active internet connection to communicate with backend

## Future Enhancements

- [ ] Implement canvas-to-image conversion for drawing mode
- [ ] Add offline mode with local model
- [ ] Support batch recognition
- [ ] Add history of recognized digits
- [ ] Implement user authentication
- [ ] Add feedback mechanism for incorrect predictions

## License

This project is part of the IHDRS system.

## Support

For issues or questions, please refer to the main IHDRS backend repository.
