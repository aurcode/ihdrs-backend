# IHDRS Mobile App

A React Native mobile application for handwritten digit recognition using the IHDRS (Intelligent Handwritten Digit Recognition System) backend. This app replicates the desktop version's features including login, drawing/upload modes, multiple prediction display, and recognition history.

## Features

- 🔐 **User Authentication**: Login system with demo credentials
- ✏️ **Draw Mode**: Draw digits directly on a canvas with adjustable brush size
- 📤 **Upload Mode**: Upload images from gallery or take photos with camera
- 🤖 **AI Recognition**: Real-time digit recognition using TensorFlow model
- 📊 **Detailed Results**: View predicted digit with confidence score
- 📈 **Probability Distribution**: See all digit probabilities with visual bars
- 📜 **Recognition History**: Track all your recognition attempts with timestamps
- 👤 **User Profile**: Display username and logout functionality

## Screenshots Features

Based on the desktop version, the mobile app includes:
- Login screen with email/password authentication
- Main dashboard with mode selection (Draw/Upload)
- Drawing canvas with brush size control
- Image picker with camera and gallery options
- Recognition results with large digit display
- Probability bars for all digits (0-9)
- Recognition history with timestamps and confidence scores

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
├── App.js                          # Main app with login flow
├── src/
│   ├── screens/
│   │   ├── LoginScreen.js          # Login screen with authentication
│   │   └── MainScreen.js           # Main dashboard screen
│   ├── components/
│   │   ├── DrawingCanvas.js        # Canvas with brush size control
│   │   ├── ImagePickerComponent.js # Image picker/camera component
│   │   └── RecognitionHistory.js   # History with probability bars
│   ├── services/
│   │   ├── authService.js          # Authentication service
│   │   └── recognitionService.js   # Recognition API service
│   └── config/
│       └── api.js                  # API configuration
├── assets/                         # App assets (icons, images)
├── package.json
└── README.md
```

## Usage

### Login
1. Enter your email and password
2. Or use demo credentials:
   - Email: demo@example.com
   - Password: password
3. Tap "Sign In"

### Draw Mode
1. Select "Draw" mode
2. Adjust brush size using the slider (5-30px)
3. Draw a digit (0-9) on the canvas
4. Tap "Recognize" to send to AI model
5. View the prediction results with probability distribution

### Upload Mode
1. Select "Upload" mode
2. Choose "Pick from Gallery" or "Take Photo"
3. Select/capture an image of a digit
4. The app automatically sends it for recognition
5. View the prediction results

### Recognition History
- Each recognition is saved in the history
- View digit, confidence score, and timestamp
- See probability bars for all digits (0-9)
- History shows most recent recognitions first

## API Integration

The app communicates with the IHDRS backend through the following endpoints:

- **POST** `/auth/login`
  - Request: `{ "email": "user@example.com", "password": "password" }`
  - Response: User data with JWT token

- **POST** `/recognition/recognize`
  - Request: `{ "imageData": "base64_encoded_image" }`
  - Response: Predicted digit, confidence score, and probability distribution

## Dependencies

- **expo**: ~54.0.20 - Expo framework
- **react-native**: 0.81.5 - React Native framework
- **expo-image-picker**: ~16.0.4 - Image picker and camera functionality
- **react-native-svg**: 15.9.0 - SVG support for drawing canvas
- **axios**: ^1.6.0 - HTTP client for API requests
- **@react-native-community/slider**: 4.5.5 - Slider component for brush size

## Troubleshooting

### Cannot connect to backend
- Ensure the backend services are running
- Check the API URL configuration in `src/config/api.js`
- For Android emulator, use `10.0.2.2` instead of `localhost`
- For physical devices, ensure they're on the same network as the backend

### Login fails
- Verify backend authentication endpoint is accessible
- Check demo credentials or create a user account
- Ensure JWT token handling is working correctly

### Image picker not working
- Grant camera and photo library permissions when prompted
- Check app permissions in device settings

### Drawing canvas not responding
- Ensure you're touching within the canvas boundaries
- Try adjusting brush size
- Clear the canvas and try again

## Features Matching Desktop Version

✅ **Login Screen**: Email/password authentication with demo credentials
✅ **Mode Selection**: Toggle between Draw and Upload modes
✅ **Drawing Canvas**: Draw digits with adjustable brush size
✅ **Image Upload**: Pick from gallery or take photo
✅ **Recognition Results**: Large digit display with confidence
✅ **Probability Display**: Visual bars showing all digit probabilities
✅ **Recognition History**: List of past recognitions with details
✅ **User Profile**: Display username and logout button
✅ **Responsive Design**: Optimized for mobile screens

## Development

To modify the app:

1. **Add new features**: Create components in `src/components/`
2. **Modify API calls**: Update services in `src/services/`
3. **Change styling**: Update styles in respective component files
4. **Configure endpoints**: Modify `src/config/api.js`
5. **Add screens**: Create new screens in `src/screens/`

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

- Drawing mode canvas-to-image conversion needs implementation for full recognition
- Upload mode is fully functional with image recognition
- Requires active internet connection to communicate with backend
- History is stored in memory (resets on app restart)

## Future Enhancements

- [ ] Implement canvas-to-image conversion for drawing mode
- [ ] Add persistent storage for recognition history
- [ ] Support batch recognition
- [ ] Add user registration
- [ ] Implement feedback mechanism for incorrect predictions
- [ ] Add dark mode support
- [ ] Export recognition history
- [ ] Add statistics dashboard

## License

This project is part of the IHDRS system.

## Support

For issues or questions, please refer to the main IHDRS backend repository.
