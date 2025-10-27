import React, { useRef, useState } from 'react';
import { View, StyleSheet, TouchableOpacity, Text, Dimensions } from 'react-native';
import Svg, { Path } from 'react-native-svg';

const { width } = Dimensions.get('window');
const CANVAS_SIZE = width - 40;

/**
 * DrawingCanvas Component
 * Allows users to draw digits on a canvas
 */
const DrawingCanvas = ({ onDrawingComplete }) => {
  const [paths, setPaths] = useState([]);
  const [currentPath, setCurrentPath] = useState('');
  const [isDrawing, setIsDrawing] = useState(false);

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
      setPaths([...paths, currentPath]);
      setCurrentPath('');
    }
    setIsDrawing(false);
  };

  const clearCanvas = () => {
    setPaths([]);
    setCurrentPath('');
    setIsDrawing(false);
  };

  const captureDrawing = async () => {
    if (paths.length === 0 && !currentPath) {
      alert('Please draw something first!');
      return;
    }

    // Create a simple representation of the drawing
    // In a real implementation, you would convert this to a proper image
    const drawingData = {
      paths: [...paths, currentPath].filter(p => p),
      canvasSize: CANVAS_SIZE,
    };

    // For now, we'll pass the drawing data
    // You might want to convert this to an actual image using a library
    onDrawingComplete(drawingData);
  };

  return (
    <View style={styles.container}>
      <View style={styles.canvasContainer}>
        <Svg
          height={CANVAS_SIZE}
          width={CANVAS_SIZE}
          style={styles.canvas}
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onTouchEnd={handleTouchEnd}
        >
          {paths.map((path, index) => (
            <Path
              key={`path-${index}`}
              d={path}
              stroke="#000"
              strokeWidth={8}
              strokeLinecap="round"
              strokeLinejoin="round"
              fill="none"
            />
          ))}
          {currentPath && (
            <Path
              d={currentPath}
              stroke="#000"
              strokeWidth={8}
              strokeLinecap="round"
              strokeLinejoin="round"
              fill="none"
            />
          )}
        </Svg>
      </View>

      <View style={styles.buttonContainer}>
        <TouchableOpacity style={styles.clearButton} onPress={clearCanvas}>
          <Text style={styles.buttonText}>Clear</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.recognizeButton} onPress={captureDrawing}>
          <Text style={styles.buttonText}>Recognize</Text>
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
    backgroundColor: '#fff',
    borderWidth: 2,
    borderColor: '#333',
    borderRadius: 10,
    overflow: 'hidden',
  },
  canvas: {
    backgroundColor: '#fff',
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
