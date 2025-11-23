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

const MainScreen = ({user, onLogout, onLogin, onProfile, onHistory, onFeedback}) => {
    const [mode, setMode] = useState('draw'); // draw | upload | multi
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

            let response;

            if (mode === 'multi') {
                response = await recognitionService.recognizeMulti(
                    base64Image,
                    inputType,
                    sessionId,
                    {
                        platform: 'mobile',
                        appVersion: '1.0.0',
                    }
                );
            } else {
                response = await recognitionService.recognizeDigit(
                    base64Image,
                    inputType,
                    sessionId,
                    {
                        platform: 'mobile',
                        appVersion: '1.0.0',
                    }
                );
            }

            if (response.success) {
                setResult(response.data);

                if (mode === 'multi') {
                    // 使用后端返回的 sequence 字段
                    const sequence = response.data.sequence ||
                        response.data.results.map(r => r.digit).join('');

                    const historyItem = {
                        id: Date.now(),
                        type: "MULTI",
                        sequence: sequence,
                        details: response.data.results.map(r => ({
                            digit: r.digit,
                            confidence: r.confidence,
                        })),
                        timestamp: new Date().toLocaleTimeString(),
                    };

                    setHistory([historyItem, ...history]);

                } else {
                    const recognitionData = response.data;
                    const historyItem = {
                        id: Date.now(),
                        digit: recognitionData.predictedDigit,
                        confidence: recognitionData.confidence,
                        probabilities: recognitionData.probabilities || null,
                        timestamp: new Date().toLocaleTimeString(),
                        inputType: inputType,
                    };
                    setHistory([historyItem, ...history]);
                }
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
                    onPress={() => {
                        if (!user) return;  // 未登录时禁止打开菜单
                        setMenuVisible(!menuVisible);
                    }}
                >
                    <Text style={[styles.menuIcon, !user && {opacity: 0.3}]}>⋮</Text>
                </TouchableOpacity>

                {menuVisible && (
                    <View style={styles.dropdownMenu}>
                        <TouchableOpacity style={styles.menuItem} onPress={() => {
                            setMenuVisible(false);
                            onProfile();
                        }}>
                            <Text style={styles.menuItemIcon}>👤</Text>
                            <Text style={styles.menuItemText}>个人中心</Text>
                        </TouchableOpacity>

                        <TouchableOpacity style={styles.menuItem} onPress={() => {
                            setMenuVisible(false);
                            onHistory();
                        }}>
                            <Text style={styles.menuItemIcon}>📊</Text>
                            <Text style={styles.menuItemText}>识别记录</Text>
                        </TouchableOpacity>

                        <TouchableOpacity style={styles.menuItem} onPress={() => {
                            setMenuVisible(false);
                            onFeedback();
                        }}>
                            <Text style={styles.menuItemIcon}>💬</Text>
                            <Text style={styles.menuItemText}>反馈记录</Text>
                        </TouchableOpacity>

                        <View style={styles.menuDivider}></View>

                        <TouchableOpacity style={styles.menuItem} onPress={() => {
                            setMenuVisible(false);
                            onLogout();
                        }}>
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
                                Draw{"\n"}(Single-digit)
                            </Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={[styles.modeButton, mode === 'multi' && styles.modeButtonActive]}
                            onPress={() => setMode('multi')}
                        >
                            <Text style={[styles.modeButtonText, mode === 'multi' && styles.modeButtonTextActive]}>
                                Draw{"\n"}(Multi-digit)
                            </Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={[styles.modeButton, mode === 'upload' && styles.modeButtonActive]}
                            onPress={() => setMode('upload')}
                        >
                            <Text style={[
                                styles.modeButtonText,
                                mode === 'upload' && styles.modeButtonTextActive
                            ]}>
                                Image{"\n"}(Single-digit)
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
                            if (mode === 'draw' || mode === 'multi') {
                                setScrollEnabled(false);
                            }
                        }}
                        // When the touch is released *from this View*...
                        onTouchEnd={() => {
                            // ...re-enable scrolling.
                            setScrollEnabled(true);
                        }}
                    >
                        {mode === 'draw' || mode === 'multi' ? (
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

                            {mode === 'multi' ? (
                                <View style={styles.resultContent}>
                                    {/* 显示完整序列 */}
                                    <Text style={styles.resultSequence}>
                                        {result.sequence || result.results?.map(r => r.digit).join('') || ''}
                                    </Text>
                                    <Text style={styles.resultSubtext}>
                                        {result.count || 0} 个数字 | 平均置信度: {
                                        result.results
                                            ? (result.results.reduce((sum, r) => sum + r.confidence, 0) / result.results.length * 100).toFixed(1)
                                            : '0'
                                    }%
                                    </Text>

                                    {/* 显示每个数字的详情 */}
                                    <View style={styles.multiResultDetails}>
                                        {result.results?.map((r, idx) => (
                                            <View key={idx} style={styles.digitResultCard}>
                                                <Text style={styles.digitResultNumber}>{r.digit}</Text>
                                                <Text style={styles.digitResultConf}>
                                                    {(r.confidence * 100).toFixed(1)}%
                                                </Text>
                                            </View>
                                        ))}
                                    </View>
                                </View>
                            ) : (
                                <View style={styles.resultContent}>
                                    <View style={styles.digitDisplay}>
                                        <Text style={styles.resultDigit}>{result.predictedDigit}</Text>
                                    </View>
                                    <Text style={styles.resultSubtext}>
                                        Confidence: {(result.confidence * 100).toFixed(1)}%
                                    </Text>
                                </View>
                            )}
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
        shadowOffset: {width: 0, height: 6},
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
        paddingVertical: 14,
        paddingHorizontal: 13,
        borderRadius: 12,
        backgroundColor: '#ffffff',
        borderWidth: 1,
        borderColor: '#d1d5db',
        alignItems: 'center',
        justifyContent: 'center',
    },
    modeButtonActive: {
        backgroundColor: '#4f46e5',
        borderColor: '#4f46e5',
        shadowOpacity: 0.15,
        elevation: 3,
    },
    modeButtonText: {
        fontSize: 15,
        fontWeight: '600',
        color: '#6b7280',
        textAlign: 'center',
        lineHeight: 20,
    },
    modeButtonTextActive: {
        color: '#ffffff',
    },
    contentCard: {
        backgroundColor: '#fff',
        borderRadius: 12,
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
    resultSequence: {
        fontSize: 48,
        fontWeight: 'bold',
        color: '#6366f1',
        letterSpacing: 4,
        marginBottom: 10,
    },
    multiResultDetails: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        justifyContent: 'center',
        marginTop: 15,
        gap: 10,
    },
    digitResultCard: {
        backgroundColor: '#f3f4f6',
        borderRadius: 8,
        padding: 12,
        minWidth: 60,
        alignItems: 'center',
        borderWidth: 2,
        borderColor: '#6366f1',
    },
    digitResultNumber: {
        fontSize: 28,
        fontWeight: 'bold',
        color: '#1f2937',
    },
    digitResultConf: {
        fontSize: 12,
        color: '#10b981',
        marginTop: 4,
    },
    resultSubtext: {
        fontSize: 14,
        color: '#6b7280',
        textAlign: 'center',
    },
});

export default MainScreen;
