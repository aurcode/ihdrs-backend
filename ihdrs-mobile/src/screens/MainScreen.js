import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
} from 'react-native';
import DrawingCanvas from '../components/DrawingCanvas';
import ImagePickerComponent from '../components/ImagePickerComponent';
import RecognitionHistory from '../components/RecognitionHistory';
import recognitionService from '../services/recognitionService';
import authService from '../services/authService';

const MainScreen = ({ user, onLogout }) => {
  const [mode, setMode] = useState('draw'); // 'draw' or 'upload'
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);

  const handleRecognition = async (base64Image) => {
    setLoading(true);
    setResult(null);

    try {
      const response = await recognitionService.recognizeDigit(base64Image);
      
      if (response.success) {
        const recognitionData = response.data.data;
        setResult(recognitionData);
        
        // Add to history
        const historyItem = {
          id: Date.now(),
          digit: recognitionData.predictedDigit,
          confidence: recognitionData.confidence,
          probabilities: recognitionData.probabilities,
          timestamp: new Date().toLocaleTimeString(),
        };
        setHistory([historyItem, ...history]);
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

  const handleLogout = () => {
    Alert.alert(
      'Logout',
      'Are you sure you want to logout?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Logout',
          style: 'destructive',
          onPress: () => {
            authService.logout();
            onLogout();
          },
        },
      ]
    );
  };

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Handwriting Recognition</Text>
        <View style={styles.headerRight}>
          <Text style={styles.userText}>{user?.username || 'User'}</Text>
          <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
            <Text style={styles.logoutButtonText}>Logout</Text>
          </TouchableOpacity>
        </View>
      </View>

      <ScrollView style={styles.content}>
        <View style={styles.mainContent}>
          <Text style={styles.pageTitle}>Handwriting Recognition</Text>
          <Text style={styles.pageSubtitle}>
            Capture or upload handwritten text for recognition
          </Text>

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
                📁 Upload Image
              </Text>
            </TouchableOpacity>
          </View>

          {/* Content Area */}
          <View style={styles.contentCard}>
            {mode === 'draw' ? (
              <DrawingCanvas onDrawingComplete={handleRecognition} />
            ) : (
              <ImagePickerComponent onImageSelected={handleRecognition} />
            )}
          </View>

          {/* Loading Indicator */}
          {loading && (
            <View style={styles.loadingContainer}>
              <ActivityIndicator size="large" color="#6366f1" />
              <Text style={styles.loadingText}>Recognizing digit...</Text>
            </View>
          )}

          {/* Recognition Result */}
          {result && !loading && (
            <View style={styles.resultCard}>
              <Text style={styles.resultTitle}>Recognition Result</Text>
              <View style={styles.resultContent}>
                <View style={styles.digitDisplay}>
                  <Text style={styles.resultDigit}>{result.predictedDigit}</Text>
                </View>
                <Text style={styles.resultSubtext}>
                  Draw or upload digit image to view recognition results
                </Text>
              </View>
            </View>
          )}

          {/* Recognition History */}
          {history.length > 0 && (
            <RecognitionHistory history={history} />
          )}
        </View>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f3f4f6',
  },
  header: {
    backgroundColor: '#6366f1',
    paddingTop: 50,
    paddingBottom: 15,
    paddingHorizontal: 20,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#fff',
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  userText: {
    color: '#fff',
    fontSize: 14,
  },
  logoutButton: {
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    paddingHorizontal: 15,
    paddingVertical: 8,
    borderRadius: 6,
  },
  logoutButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  content: {
    flex: 1,
  },
  mainContent: {
    padding: 20,
  },
  pageTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#1f2937',
    textAlign: 'center',
    marginBottom: 8,
  },
  pageSubtitle: {
    fontSize: 16,
    color: '#6b7280',
    textAlign: 'center',
    marginBottom: 30,
  },
  modeSelector: {
    flexDirection: 'row',
    marginBottom: 20,
    gap: 10,
  },
  modeButton: {
    flex: 1,
    paddingVertical: 15,
    paddingHorizontal: 20,
    borderRadius: 8,
    backgroundColor: '#fff',
    borderWidth: 2,
    borderColor: '#e5e7eb',
    alignItems: 'center',
  },
  modeButtonActive: {
    backgroundColor: '#6366f1',
    borderColor: '#6366f1',
  },
  modeButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#6b7280',
  },
  modeButtonTextActive: {
    color: '#fff',
  },
  contentCard: {
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
    padding: 30,
    backgroundColor: '#fff',
    borderRadius: 12,
    marginBottom: 20,
  },
  loadingText: {
    marginTop: 10,
    fontSize: 16,
    color: '#6b7280',
  },
  resultCard: {
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
    color: '#1f2937',
    marginBottom: 15,
  },
  resultContent: {
    alignItems: 'center',
  },
  digitDisplay: {
    width: 120,
    height: 120,
    backgroundColor: '#6366f1',
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 15,
  },
  resultDigit: {
    fontSize: 64,
    fontWeight: 'bold',
    color: '#fff',
  },
  resultSubtext: {
    fontSize: 14,
    color: '#6b7280',
    textAlign: 'center',
  },
});

export default MainScreen;
