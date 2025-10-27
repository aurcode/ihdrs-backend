import React, { useState } from 'react';
import { StatusBar } from 'expo-status-bar';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
} from 'react-native';
import DrawingCanvas from './src/components/DrawingCanvas';
import ImagePickerComponent from './src/components/ImagePickerComponent';
import recognitionService from './src/services/recognitionService';

export default function App() {
  const [mode, setMode] = useState('draw'); // 'draw' or 'upload'
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const handleDrawingComplete = async (drawingData) => {
    Alert.alert(
      'Drawing Recognition',
      'Note: Drawing recognition requires converting the canvas to an image. For now, please use the image upload feature for full functionality.',
      [{ text: 'OK' }]
    );
    
    // In a production app, you would convert the drawing to a base64 image here
    // For demonstration, we'll show a placeholder
    console.log('Drawing data:', drawingData);
  };

  const handleImageSelected = async (base64Image) => {
    setLoading(true);
    setResult(null);

    try {
      const response = await recognitionService.recognizeDigit(base64Image);
      
      if (response.success) {
        setResult(response.data);
        Alert.alert(
          'Recognition Complete',
          `Predicted Digit: ${response.data.data?.predictedDigit || 'N/A'}\nConfidence: ${
            response.data.data?.confidence
              ? (response.data.data.confidence * 100).toFixed(2) + '%'
              : 'N/A'
          }`,
          [{ text: 'OK' }]
        );
      } else {
        Alert.alert('Error', response.error || 'Recognition failed');
      }
    } catch (error) {
      console.error('Recognition error:', error);
      Alert.alert('Error', 'Failed to recognize digit. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <StatusBar style="auto" />
      
      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.title}>🔢 Digit Recognition</Text>
          <Text style={styles.subtitle}>Draw or upload a digit (0-9)</Text>
        </View>

        {/* Mode Selector */}
        <View style={styles.modeSelector}>
          <TouchableOpacity
            style={[styles.modeButton, mode === 'draw' && styles.modeButtonActive]}
            onPress={() => setMode('draw')}
          >
            <Text style={[styles.modeButtonText, mode === 'draw' && styles.modeButtonTextActive]}>
              ✏️ Draw
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.modeButton, mode === 'upload' && styles.modeButtonActive]}
            onPress={() => setMode('upload')}
          >
            <Text style={[styles.modeButtonText, mode === 'upload' && styles.modeButtonTextActive]}>
              📤 Upload
            </Text>
          </TouchableOpacity>
        </View>

        {/* Content Area */}
        <View style={styles.contentArea}>
          {mode === 'draw' ? (
            <DrawingCanvas onDrawingComplete={handleDrawingComplete} />
          ) : (
            <ImagePickerComponent onImageSelected={handleImageSelected} />
          )}
        </View>

        {/* Loading Indicator */}
        {loading && (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color="#4CAF50" />
            <Text style={styles.loadingText}>Recognizing digit...</Text>
          </View>
        )}

        {/* Result Display */}
        {result && !loading && (
          <View style={styles.resultContainer}>
            <Text style={styles.resultTitle}>Recognition Result</Text>
            <View style={styles.resultContent}>
              <Text style={styles.resultDigit}>
                {result.data?.predictedDigit ?? 'N/A'}
              </Text>
              <Text style={styles.resultConfidence}>
                Confidence: {result.data?.confidence 
                  ? (result.data.confidence * 100).toFixed(2) + '%'
                  : 'N/A'}
              </Text>
              {result.data?.probabilities && (
                <View style={styles.probabilitiesContainer}>
                  <Text style={styles.probabilitiesTitle}>All Probabilities:</Text>
                  {result.data.probabilities.map((prob, index) => (
                    <Text key={index} style={styles.probabilityText}>
                      {index}: {(prob * 100).toFixed(2)}%
                    </Text>
                  ))}
                </View>
              )}
            </View>
          </View>
        )}

        {/* Instructions */}
        <View style={styles.instructions}>
          <Text style={styles.instructionsTitle}>📋 Instructions:</Text>
          <Text style={styles.instructionsText}>
            • Draw Mode: Draw a digit on the canvas{'\n'}
            • Upload Mode: Pick an image or take a photo{'\n'}
            • Make sure the digit is clear and centered{'\n'}
            • The AI model works best with handwritten digits
          </Text>
        </View>

        {/* API Configuration Note */}
        <View style={styles.configNote}>
          <Text style={styles.configNoteText}>
            ⚙️ Configure API endpoint in src/config/api.js
          </Text>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  scrollContent: {
    padding: 20,
    paddingTop: 50,
  },
  header: {
    alignItems: 'center',
    marginBottom: 30,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
  },
  modeSelector: {
    flexDirection: 'row',
    marginBottom: 20,
    gap: 10,
  },
  modeButton: {
    flex: 1,
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 8,
    backgroundColor: '#fff',
    borderWidth: 2,
    borderColor: '#ddd',
    alignItems: 'center',
  },
  modeButtonActive: {
    backgroundColor: '#4CAF50',
    borderColor: '#4CAF50',
  },
  modeButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#666',
  },
  modeButtonTextActive: {
    color: '#fff',
  },
  contentArea: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  loadingContainer: {
    alignItems: 'center',
    padding: 20,
  },
  loadingText: {
    marginTop: 10,
    fontSize: 16,
    color: '#666',
  },
  resultContainer: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  resultTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 15,
    textAlign: 'center',
  },
  resultContent: {
    alignItems: 'center',
  },
  resultDigit: {
    fontSize: 72,
    fontWeight: 'bold',
    color: '#4CAF50',
    marginBottom: 10,
  },
  resultConfidence: {
    fontSize: 18,
    color: '#666',
    marginBottom: 15,
  },
  probabilitiesContainer: {
    width: '100%',
    marginTop: 15,
    padding: 15,
    backgroundColor: '#f9f9f9',
    borderRadius: 8,
  },
  probabilitiesTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10,
  },
  probabilityText: {
    fontSize: 14,
    color: '#666',
    marginVertical: 2,
  },
  instructions: {
    backgroundColor: '#e3f2fd',
    borderRadius: 12,
    padding: 15,
    marginBottom: 20,
  },
  instructionsTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#1976d2',
    marginBottom: 8,
  },
  instructionsText: {
    fontSize: 14,
    color: '#555',
    lineHeight: 22,
  },
  configNote: {
    backgroundColor: '#fff3cd',
    borderRadius: 8,
    padding: 12,
    marginBottom: 20,
  },
  configNoteText: {
    fontSize: 12,
    color: '#856404',
    textAlign: 'center',
  },
});
