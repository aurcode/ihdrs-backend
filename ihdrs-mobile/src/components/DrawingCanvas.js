import React, { useRef, useState } from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  Text,
  Dimensions,
  Alert, // Import Alert for native alerts
} from 'react-native';
import Svg, { Path } from 'react-native-svg';
import Slider from '@react-native-community/slider';
// Import react-native-view-shot to capture the drawing as an image
import ViewShot from 'react-native-view-shot';

const { width } = Dimensions.get('window');
const CANVAS_SIZE = width - 40;
// Define a white background color. ML models work best with non-transparent images.
const CANVAS_BACKGROUND_COLOR = '#FFFFFF';

/**
 * DrawingCanvas Component
 * Allows users to draw digits on a canvas with adjustable brush size
 * Captures the drawing as a base64 image for recognition.
 */
const DrawingCanvas = ({ onDrawingComplete }) => {
  const [paths, setPaths] = useState([]);
  const [currentPath, setCurrentPath] = useState('');
  const [isDrawing, setIsDrawing] = useState(false);
  const [brushSize, setBrushSize] = useState(15);

  // Create a ref to attach to the ViewShot component
  const viewShotRef = useRef(null);

  const handleTouchStart = (event) => {
    const { locationX, locationY } = event.nativeEvent;
    setCurrentPath(`M${locationX},${locationY}`);
    setIsDrawing(true);
  };

  const handleTouchMove = (event) => {
    if (!isDrawing) return;

    const { locationX, locationY } = event.nativeEvent;
    setCurrentPath((prevPath) => `${prevPath} L${locationX},${locationY}`);
  };

  const handleTouchEnd = () => {
    if (currentPath) {
      setPaths([...paths, { path: currentPath, strokeWidth: brushSize }]);
      setCurrentPath('');
    }
    setIsDrawing(false);
  };

  const clearCanvas = () => {
    setPaths([]);
    setCurrentPath('');
    setIsDrawing(false);
  };

  /**
   * Capture the drawing as a base64 encoded image
   */
  const captureDrawing = async () => {
    if (paths.length === 0 && !currentPath) {
      // Use Alert.alert for cross-platform alerts
      Alert.alert('Empty Canvas', 'Please draw something first!');
      return;
    }

    try {
      // Use the ref to capture the component
      // This returns a promise that resolves with the base64 string
      const base64Image = await viewShotRef.current.capture({
        format: 'png', // Output format
        quality: 0.9, // Image quality
        result: 'base64', // Return a base64 string
        // Note: Your ML model might expect a specific size (e.g., 28x28 for MNIST).
        // The backend service might handle resizing. If not, you may need to
        // resize the image here or on the backend.
        // We pass the full-size canvas capture for now.
      });

      // Pass the base64 string directly to the parent's handler
      onDrawingComplete(base64Image);
    } catch (error) {
      console.error('Failed to capture drawing:', error);
      Alert.alert('Error', 'Could not capture the drawing. Please try again.');
    }
  };

  return (
    <View style={styles.container}>
      {/* Wrap the Svg component in ViewShot.
          This ViewShot component is what we will capture.
       */}
      <ViewShot
        ref={viewShotRef}
        style={styles.canvasContainer}
        options={{ format: 'png', quality: 1.0, result: 'base64' }}
      >
        <Svg
          height={CANVAS_SIZE}
          width={CANVAS_SIZE}
          style={styles.canvas} // The background color is now on the container
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onTouchEnd={handleTouchEnd}
          // **FIX:** Add responder props here. This makes the Svg component
          // the primary touch handler, preventing parent ScrollViews
          // from intercepting the touch gesture and scrolling.
          onStartShouldSetResponder={() => true}
          onMoveShouldSetResponder={() => true}
        >
          {/* Add a white background rectangle.
              This is crucial so the captured PNG is not transparent.
           */}
          <Path
            d={`M0,0 H${CANVAS_SIZE} V${CANVAS_SIZE} H0 Z`}
            fill={CANVAS_BACKGROUND_COLOR}
          />

          {/* Render all completed paths */}
          {paths.map((pathObj, index) => (
            <Path
              key={`path-${index}`}
              d={pathObj.path}
              stroke="#000"
              strokeWidth={pathObj.strokeWidth}
              strokeLinecap="round"
              strokeLinejoin="round"
              fill="none"
            />
          ))}

          {/* Render the current path being drawn */}
          {currentPath && (
            <Path
              d={currentPath}
              stroke="#000"
              strokeWidth={brushSize}
              strokeLinecap="round"
              strokeLinejoin="round"
              fill="none"
            />
          )}
        </Svg>
      </ViewShot>

      {/* Brush Size Slider */}
      <View style={styles.brushSizeContainer}>
        <Text style={styles.brushSizeLabel}>Brush Size:</Text>
        <Slider
          style={styles.slider}
          minimumValue={5}
          maximumValue={30}
          value={brushSize}
          onValueChange={setBrushSize}
          minimumTrackTintColor="#6366f1"
          maximumTrackTintColor="#d1d5db"
          thumbTintColor="#6366f1"
        />
        <Text style={styles.brushSizeValue}>{Math.round(brushSize)}px</Text>
      </View>

      {/* Action Buttons */}
      <View style={styles.buttonContainer}>
        <TouchableOpacity style={styles.clearButton} onPress={clearCanvas}>
          <Text style={styles.buttonText}>🗑️ Clear</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.recognizeButton}
          onPress={captureDrawing}
        >
          <Text style={styles.buttonText}>🔍 Recognize</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    marginVertical: 20,
  },
  canvasContainer: {
    backgroundColor: CANVAS_BACKGROUND_COLOR, // Set background color here
    borderWidth: 2,
    borderColor: '#333',
    borderRadius: 10,
    overflow: 'hidden', // Ensures the Svg corners are rounded
  },
  canvas: {
    // Background color is inherited from the container
  },
  brushSizeContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 15,
    paddingHorizontal: 10,
  },
  brushSizeLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#374151',
    marginRight: 10,
  },
  slider: {
    flex: 1,
    height: 40,
  },
  brushSizeValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#6366f1',
    marginLeft: 10,
    minWidth: 45,
  },
  buttonContainer: {
    flexDirection: 'row',
    marginTop: 20,
    gap: 15,
  },
  clearButton: {
    backgroundColor: '#ff6b6b',
    paddingHorizontal: 30,
    paddingVertical: 12,
    borderRadius: 8,
    minWidth: 120,
    alignItems: 'center',
  },
  recognizeButton: {
    backgroundColor: '#4CAF50',
    paddingHorizontal: 30,
    paddingVertical: 12,
    borderRadius: 8,
    minWidth: 120,
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default DrawingCanvas;