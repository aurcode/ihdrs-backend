import React, { useRef, useState } from 'react';
import { View, StyleSheet, TouchableOpacity, Text, Dimensions } from 'react-native';
import Svg, { Path } from 'react-native-svg';
import Slider from '@react-native-community/slider';

const { width } = Dimensions.get('window');
const CANVAS_SIZE = width - 40;

/**
 * DrawingCanvas Component
 * Allows users to draw digits on a canvas with adjustable brush size
 */
const DrawingCanvas = ({ onDrawingComplete }) => {
  const [paths, setPaths] = useState([]);
  const [currentPath, setCurrentPath] = useState('');
  const [isDrawing, setIsDrawing] = useState(false);
  const [brushSize, setBrushSize] = useState(15);

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
      </View>

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

      <View style={styles.buttonContainer}>
        <TouchableOpacity style={styles.clearButton} onPress={clearCanvas}>
          <Text style={styles.buttonText}>🗑️ Clear</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.recognizeButton} onPress={captureDrawing}>
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
    backgroundColor: '#fff',
    borderWidth: 2,
    borderColor: '#333',
    borderRadius: 10,
    overflow: 'hidden',
  },
  canvas: {
    backgroundColor: '#fff',
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
