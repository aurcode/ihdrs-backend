import React, {useMemo, useState} from 'react';
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
import { v4 as uuidv4 } from 'uuid';

const MainScreen = ({user, onLogout, onLogin, onProfile, onHistory, onFeedback}) => {
    const [mode, setMode] = useState('draw'); // 'draw' or 'upload'
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);
    const [history, setHistory] = useState([]);
    const [menuVisible, setMenuVisible] = useState(false);
    const sessionId = useMemo(
        () => `mobile-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`,
        []
    );

    // **FIX 1: Add state to control the ScrollView**
    const [scrollEnabled, setScrollEnabled] = useState(true);

    /**
     * Handles the recognition request for both drawing and uploading.
     * @param {string} base64Image - The base64 encoded image string.
     */
    const handleRecognition = async (base64Image) => {
            if (!base64Image) {
                Alert.alert('Error', 'No image data received.');
                return;
            }

            setLoading(true);
            setResult(null);

            try {
                const inputType = mode === 'draw' ? 'CANVAS' : 'UPLOAD';
                const response = await recognitionService.recognizeDigit(
                    base64Image,
                    inputType,
                    sessionId,
                    null,
                    {
                        platform: 'mobile',
                        appVersion: '1.0.0',
                    }
                );

                if (response.success) {
                    // 新的 recognitionService 已经把 Java 后端 data 展平了
                    const recognitionData = response.data;

                    setResult(recognitionData);

                    const historyItem = {
                        id: Date.now(),
                        digit: recognitionData.predictedDigit,
                        confidence: recognitionData.confidence,
                        probabilities: recognitionData.probabilities || null,
                        timestamp: new Date().toLocaleTimeString(),
                        inputType: inputType,
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
                {text: 'Cancel', style: 'cancel'},
                {
                    text: 'Logout',
                    style: 'destructive',
                    onPress: () => {
                        // authService.logout(); // Assuming this exists
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
                    {user ? (
                        <Text
                            style={styles.userText}
                            numberOfLines={1}
                            ellipsizeMode="tail"
                        >
                            {user.userInfo.username}
                        </Text>
                    ) : (
                        <TouchableOpacity style={styles.logoutButton} onPress={onLogin}>
                            <Text style={styles.logoutButtonText}>Login</Text>
                        </TouchableOpacity>
                    )}
                </View>
                <TouchableOpacity
                    style={styles.menuButton}
                    onPress={() => setMenuVisible(!menuVisible)}
                >
                    <Text style={styles.menuIcon}>⋮</Text>
                </TouchableOpacity>

                {menuVisible && (
                    <View style={styles.dropdownMenu}>
                        <TouchableOpacity style={styles.menuItem} onPress={() => { setMenuVisible(false); onProfile(); }}>
                            <Text style={styles.menuItemIcon}>👤</Text>
                            <Text style={styles.menuItemText}>个人中心</Text>
                        </TouchableOpacity>

                        <TouchableOpacity style={styles.menuItem} onPress={() => { setMenuVisible(false); onHistory(); }}>
                            <Text style={styles.menuItemIcon}>📊</Text>
                            <Text style={styles.menuItemText}>识别记录</Text>
                        </TouchableOpacity>

                        <TouchableOpacity style={styles.menuItem} onPress={() => { setMenuVisible(false); onFeedback(); }}>
                            <Text style={styles.menuItemIcon}>💬</Text>
                            <Text style={styles.menuItemText}>反馈记录</Text>
                        </TouchableOpacity>

                        <View style={styles.menuDivider}></View>

                        <TouchableOpacity style={styles.menuItem} onPress={() => { setMenuVisible(false); onLogout(); }}>
                            <Text style={styles.menuItemIcon}>🚪</Text>
                            <Text style={styles.menuItemText}>Logout</Text>
                        </TouchableOpacity>
                    </View>
                )}
            </View>
            {/* **FIX 2: Pass the scrollEnabled state to the ScrollView** */}
            <ScrollView
                style={styles.content}
                scrollEnabled={scrollEnabled}
            >
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
                    {/* **FIX 3: Add touch handlers to the wrapper View** */}
                    <View
                        style={styles.contentCard}
                        // When a touch starts *inside this View*...
                        onTouchStart={() => {
                            // ...and we are in 'draw' mode, disable scrolling.
                            if (mode === 'draw') {
                                setScrollEnabled(false);
                            }
                        }}
                        // When the touch is released *from this View*...
                        onTouchEnd={() => {
                            // ...re-enable scrolling.
                            setScrollEnabled(true);
                        }}
                    >
                        {mode === 'draw' ? (
                            <DrawingCanvas onDrawingComplete={handleRecognition}/>
                        ) : (
                            <ImagePickerComponent onImageSelected={handleRecognition}/>
                        )}
                    </View>

                    {/* Loading Indicator */}
                    {loading && (
                        <View style={styles.loadingContainer}>
                            <ActivityIndicator size="large" color="#6366f1"/>
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
                                    Confidence: {(result.confidence * 100).toFixed(1)}%
                                </Text>
                            </View>
                        </View>
                    )}

                    {/* Recognition History */}
                    {history.length > 0 && (
                        <RecognitionHistory history={history}/>
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
    menuButton: {
        width: 36,
        height: 36,
        borderRadius: 18,
        backgroundColor: 'rgba(255,255,255,0.2)',
        justifyContent: 'center',
        alignItems: 'center',
    },
    menuIcon: {
        fontSize: 22,
        color: '#fff',
        marginTop: -2,
    },
    dropdownMenu: {
        position: 'absolute',
        top: 90,
        right: 0,
        backgroundColor: '#fff',
        borderRadius: 12,
        paddingVertical: 8,
        width: 160,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 0.25,
        shadowRadius: 12,
        elevation: 10,
        zIndex: 10000,
    },
    menuItem: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 10,
        paddingHorizontal: 14,
    },
    menuItemIcon: {
        fontSize: 18,
        marginRight: 10,
    },
    menuItemText: {
        fontSize: 16,
        color: '#1f2937',
        fontWeight: '600',
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
        maxWidth: 60,
    },
    profileButton: {
        width: 24,
        height: 24,
        borderRadius: 12,
        backgroundColor: 'rgba(255, 255, 255, 0.2)',
        justifyContent: 'center',
        alignItems: 'center',
        marginLeft: 5
    },
    profileIcon: {
        fontSize: 10,
    },
    historyButton: {
        width: 24,
        height: 24,
        borderRadius: 12,
        backgroundColor: 'rgba(255, 255, 255, 0.2)',
        justifyContent: 'center',
        alignItems: 'center',
    },
    historyIcon: {
        fontSize: 20,
    },
    feedbackButton: {
        width: 24,
        height: 24,
        borderRadius: 12,
        backgroundColor: 'rgba(255, 255, 255, 0.2)',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 8,
    },
    feedbackIcon: {
        fontSize: 20,
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
        shadowOffset: {width: 0, height: 2},
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
        shadowOffset: {width: 0, height: 2},
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