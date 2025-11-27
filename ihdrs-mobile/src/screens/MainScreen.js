import React, {useMemo, useState, useRef, useEffect} from 'react';
import {
    View,
    Text,
    StyleSheet,
    ScrollView,
    TouchableOpacity,
    TouchableWithoutFeedback,
    ActivityIndicator,
    Alert,
    StatusBar,
    Platform,
    Dimensions,
    Animated,
    Easing,
} from 'react-native';
import DrawingCanvas from '../components/DrawingCanvas';
import ImagePickerComponent from '../components/ImagePickerComponent';
import RecognitionHistory from '../components/RecognitionHistory';
import recognitionService from '../services/recognitionService';

const { width, height } = Dimensions.get('window');

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

    // 动画引用
    const fadeAnim = useRef(new Animated.Value(0)).current;
    const backgroundY = useRef(new Animated.Value(0)).current;
    const resultScale = useRef(new Animated.Value(0)).current;
    const particles = useRef([]);

    useEffect(() => {
        // 初始淡入动画
        Animated.timing(fadeAnim, {
            toValue: 1,
            duration: 800,
            useNativeDriver: true,
            easing: Easing.out(Easing.back(1.2)),
        }).start();

        // 背景浮动动画
        Animated.loop(
            Animated.sequence([
                Animated.timing(backgroundY, {
                    toValue: -20,
                    duration: 4000,
                    useNativeDriver: true,
                    easing: Easing.inOut(Easing.sin),
                }),
                Animated.timing(backgroundY, {
                    toValue: 20,
                    duration: 4000,
                    useNativeDriver: true,
                    easing: Easing.inOut(Easing.sin),
                }),
            ])
        ).start();

        // 创建粒子
        createParticles();
    }, [fadeAnim, backgroundY]);

    // 结果出现动画
    useEffect(() => {
        if (result) {
            Animated.spring(resultScale, {
                toValue: 1,
                tension: 50,
                friction: 7,
                useNativeDriver: true,
            }).start();
        } else {
            resultScale.setValue(0);
        }
    }, [result]);

    const createParticles = () => {
        const newParticles = [];
        for (let i = 0; i < 15; i++) {
            newParticles.push({
                x: Math.random() * width,
                y: Math.random() * height,
                size: Math.random() * 3 + 1,
                speed: Math.random() * 0.5 + 0.2,
                opacity: Math.random() * 0.2 + 0.1,
            });
        }
        particles.current = newParticles;
    };

    // 渲染粒子背景
    const renderParticles = () => {
        return particles.current.map((particle, index) => (
            <Animated.View
                key={index}
                style={[
                    styles.particle,
                    {
                        left: particle.x,
                        top: particle.y + backgroundY,
                        width: particle.size,
                        height: particle.size,
                        opacity: particle.opacity,
                    },
                ]}
            />
        ));
    };

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
            <StatusBar barStyle="dark-content" backgroundColor="transparent" translucent />

            {/* 动态背景 */}
            <Animated.View
                style={[
                    styles.backgroundGradient,
                    {
                        opacity: fadeAnim,
                        transform: [{ translateY: backgroundY }]
                    }
                ]}
            >
                {renderParticles()}
                <View style={styles.circleTop} />
                <View style={styles.circleBottom} />
                <View style={styles.circleMiddle} />
            </Animated.View>

            {/* Header with Logo and Title */}
            <Animated.View
                style={[
                    styles.headerSection,
                    {
                        opacity: fadeAnim.interpolate({
                            inputRange: [0, 0.7, 1],
                            outputRange: [0, 0, 1]
                        }),
                        transform: [{
                            translateY: fadeAnim.interpolate({
                                inputRange: [0, 0.7, 1],
                                outputRange: [-30, -30, 0]
                            })
                        }]
                    }
                ]}
            >
                <Animated.View
                    style={[
                        styles.headerRow,
                        {
                            opacity: fadeAnim.interpolate({
                                inputRange: [0, 1],
                                outputRange: [0, 1]
                            })
                        }
                    ]}
                >
                    {/* Icon */}
                    <Animated.View
                        style={[
                            styles.logoContainer,
                            {
                                opacity: fadeAnim.interpolate({
                                    inputRange: [0, 0.5, 1],
                                    outputRange: [0, 0.5, 1]
                                }),
                                transform: [{
                                    scale: fadeAnim.interpolate({
                                        inputRange: [0, 1],
                                        outputRange: [0.5, 1]
                                    })
                                }]
                            }
                        ]}
                    >
                        <View style={styles.logoCircle}>
                            <Text style={styles.logoText}>✍️</Text>
                        </View>
                    </Animated.View>

                    {/* Text group */}
                    <View style={styles.textColumn}>
                        <Animated.Text
                            style={[
                                styles.headerTitle,
                                {
                                    opacity: fadeAnim.interpolate({
                                        inputRange: [0, 0.7, 1],
                                        outputRange: [0, 0, 1]
                                    }),
                                    transform: [{
                                        translateY: fadeAnim.interpolate({
                                            inputRange: [0, 0.7, 1],
                                            outputRange: [20, 20, 0]
                                        })
                                    }]
                                }
                            ]}
                        >
                            手写数字识别
                        </Animated.Text>

                        <Animated.Text
                            style={[
                                styles.headerSubtitle,
                                {
                                    opacity: fadeAnim.interpolate({
                                        inputRange: [0, 0.8, 1],
                                        outputRange: [0, 0, 1]
                                    }),
                                    transform: [{
                                        translateY: fadeAnim.interpolate({
                                            inputRange: [0, 0.8, 1],
                                            outputRange: [20, 20, 0]
                                        })
                                    }]
                                }
                            ]}
                        >
                            Handwritten Digit Recognition
                        </Animated.Text>
                    </View>
                </Animated.View>


                {/* User Actions */}
                <View style={styles.headerActions}>
                    {user ? (
                        <View style={styles.userTextContainer}>
                            <Text
                                style={styles.userText}
                                numberOfLines={1}
                                ellipsizeMode="tail"
                            >
                                {user.userInfo.username}
                            </Text>
                        </View>
                    ) : (
                        <TouchableOpacity style={styles.loginButton} onPress={onLogin}>
                            <Text style={styles.loginButtonText}>登录</Text>
                        </TouchableOpacity>
                    )}
                    <TouchableOpacity
                        style={styles.menuButton}
                        onPress={() => {
                            if (!user) return;  // 未登录时禁止打开菜单
                            setMenuVisible(!menuVisible);
                        }}
                    >
                        <Text style={[styles.menuIcon, !user && {opacity: 0.3}]}>⋮</Text>
                    </TouchableOpacity>
                </View>
            </Animated.View>

            {/* 菜单覆盖层和下拉菜单 - 放在最外层 */}
            {menuVisible && (
                <>
                    {/* 覆盖层 - 点击空白处关闭菜单 */}
                    <TouchableWithoutFeedback onPress={() => setMenuVisible(false)}>
                        <View style={styles.menuOverlay} />
                    </TouchableWithoutFeedback>
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
                            Alert.alert("提示", "已退出登录");
                            onLogout();
                        }}>
                            <Text style={styles.menuItemIcon}>🚪</Text>
                            <Text style={styles.menuItemText}>退出登录</Text>
                        </TouchableOpacity>
                    </View>
                </>
            )}

            {/* **FIX 2: Pass the scrollEnabled state to the ScrollView** */}
            <ScrollView
                style={styles.content}
                scrollEnabled={scrollEnabled}
                showsVerticalScrollIndicator={false}
            >
                <Animated.View
                    style={[
                        styles.mainContent,
                        {
                            opacity: fadeAnim.interpolate({
                                inputRange: [0, 0.6, 1],
                                outputRange: [0, 0, 1]
                            }),
                            transform: [{
                                translateY: fadeAnim.interpolate({
                                    inputRange: [0, 0.6, 1],
                                    outputRange: [30, 30, 0]
                                })
                            }]
                        }
                    ]}
                >

                    {/* Mode Selector */}
                    <View style={styles.modeSelector}>
                        <TouchableOpacity
                            style={[styles.modeButton, mode === 'draw' && styles.modeButtonActive]}
                            onPress={() => setMode('draw')}
                            activeOpacity={0.8}
                        >
                            <Text style={[styles.modeButtonText, mode === 'draw' && styles.modeButtonTextActive]}>
                                手写{"\n"}(单数字)
                            </Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={[styles.modeButton, mode === 'multi' && styles.modeButtonActive]}
                            onPress={() => setMode('multi')}
                            activeOpacity={0.8}
                        >
                            <Text style={[styles.modeButtonText, mode === 'multi' && styles.modeButtonTextActive]}>
                                手写{"\n"}(连续数字)
                            </Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={[styles.modeButton, mode === 'upload' && styles.modeButtonActive]}
                            onPress={() => setMode('upload')}
                            activeOpacity={0.8}
                        >
                            <Text style={[
                                styles.modeButtonText,
                                mode === 'upload' && styles.modeButtonTextActive
                            ]}>
                                照片{"\n"}(单数字)
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
                        <Animated.View
                            style={[
                                styles.loadingContainer,
                                {
                                    opacity: fadeAnim
                                }
                            ]}
                        >
                            <ActivityIndicator size="large" color="#2563eb"/>
                            <Text style={styles.loadingText}>正在识别中...</Text>
                        </Animated.View>
                    )}

                    {/* Recognition Result */}
                    {result && !loading && (
                        <Animated.View
                            style={[
                                styles.resultCard,
                                {
                                    transform: [{ scale: resultScale }],
                                    opacity: resultScale
                                }
                            ]}
                        >
                            <Text style={styles.resultTitle}>识别结果</Text>

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
                                        置信度: {(result.confidence * 100).toFixed(1)}%
                                    </Text>
                                </View>
                            )}
                        </Animated.View>
                    )}

                    {/* Recognition History */}
                    {history.length > 0 && (
                        <RecognitionHistory history={history}/>
                    )}
                </Animated.View>
            </ScrollView>
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#f8fafc',
    },
    backgroundGradient: {
        position: 'absolute',
        width: '100%',
        height: '100%',
        overflow: 'hidden',
    },
    particle: {
        position: 'absolute',
        borderRadius: 50,
        backgroundColor: 'rgba(59, 130, 246, 0.3)',
    },
    circleTop: {
        position: 'absolute',
        width: width * 1.8,
        height: width * 1.8,
        borderRadius: width * 0.9,
        backgroundColor: 'rgba(147, 197, 253, 0.2)',
        top: -width * 0.6,
        right: -width * 0.4,
    },
    circleBottom: {
        position: 'absolute',
        width: width * 1.5,
        height: width * 1.5,
        borderRadius: width * 0.75,
        backgroundColor: 'rgba(191, 219, 254, 0.2)',
        bottom: -width * 0.5,
        left: -width * 0.4,
    },
    circleMiddle: {
        position: 'absolute',
        width: width * 1.2,
        height: width * 1.2,
        borderRadius: width * 0.6,
        backgroundColor: 'rgba(224, 242, 254, 0.3)',
        top: '40%',
        left: '20%',
    },
    headerSection: {
        paddingTop: Platform.OS === 'ios' ? 75 : 55
        ,
        paddingBottom: 15,
        paddingHorizontal: 20,
        alignItems: 'center',
        position: 'relative',
        zIndex: 100,
    },
    logoContainer: {
        marginBottom: 12,
        marginTop: 20,
    },
    logoCircle: {
        width: 80,
        height: 80,
        borderRadius: 40,
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        justifyContent: 'center',
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 8 },
        shadowOpacity: 0.15,
        shadowRadius: 20,
        elevation: 8,
        borderWidth: 3,
        borderColor: 'rgba(59, 130, 246, 0.2)',
    },
    logoText: {
        fontSize: 40,
    },
    headerTitle: {
        fontSize: 32,
        fontWeight: 'bold',
        color: '#1e293b',
        marginBottom: 4,
        textAlign: 'center',
        textShadowColor: 'rgba(0, 0, 0, 0.1)',
        textShadowOffset: { width: 0, height: 2 },
        textShadowRadius: 4,
    },
    headerSubtitle: {
        fontSize: 16,
        color: '#64748b',
        textAlign: 'center',
        fontWeight: '500',
        marginBottom: 12,
    },
    headerActions: {
        position: 'absolute',
        top: Platform.OS === 'ios' ? 50 : 30,
        right: 20,
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
    },
    menuButton: {
        width: 36,
        height: 36,
        borderRadius: 18,
        backgroundColor: 'rgba(255, 255, 255, 0.4)',
        justifyContent: 'center',
        alignItems: 'center',
        borderWidth: 2,
        borderColor: 'rgba(255, 255, 255, 0.6)',
        shadowColor: '#2563eb',
        shadowOffset: { width: 0, height: 3 },
        shadowOpacity: 0.25,
        shadowRadius: 6,
        elevation: 5,
    },
    menuIcon: {
        fontSize: 22,
        color: '#1e293b',
        marginTop: -2,
        fontWeight: '700',
        letterSpacing: 2,
    },
    menuOverlay: {
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 9999,
    },
    dropdownMenu: {
        position: 'absolute',
        top: Platform.OS === 'ios' ? 50 : 30,
        right: 20,
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 16,
        paddingVertical: 8,
        width: 160,
        shadowColor: '#000',
        shadowOffset: {width: 0, height: 8},
        shadowOpacity: 0.15,
        shadowRadius: 20,
        elevation: 10,
        zIndex: 10000,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.5)',
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
    userTextContainer: {
        height: 36,
        minWidth: 60,
        maxWidth: 100,
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        paddingHorizontal: 14,
        borderRadius: 18,
        borderWidth: 2,
        borderColor: 'rgba(37, 99, 235, 0.3)',
        shadowColor: '#2563eb',
        shadowOffset: { width: 0, height: 3 },
        shadowOpacity: 0.2,
        shadowRadius: 6,
        elevation: 4,
        justifyContent: 'center',
        alignItems: 'center',
        overflow: 'hidden',
    },
    userText: {
        color: '#1e293b',
        fontSize: 15,
        fontWeight: '700',
        textAlign: 'center',
    },
    loginButton: {
        backgroundColor: '#2563eb',
        paddingHorizontal: 16,
        paddingVertical: 0,
        height: 36,
        borderRadius: 18,
        borderWidth: 2,
        borderColor: 'rgba(255, 255, 255, 0.5)',
        shadowColor: '#2563eb',
        shadowOffset: { width: 0, height: 3 },
        shadowOpacity: 0.4,
        shadowRadius: 6,
        elevation: 5,
        justifyContent: 'center',
        alignItems: 'center',
    },
    loginButtonText: {
        color: '#fff',
        fontSize: 14,
        fontWeight: '700',
        textShadowColor: 'rgba(0, 0, 0, 0.2)',
        textShadowOffset: { width: 0, height: 1 },
        textShadowRadius: 2,
    },
    content: {
        flex: 1,
    },
    mainContent: {
        padding: 20,
        paddingTop: 0,
        paddingBottom: 20,
    },
    modeSelector: {
        flexDirection: 'row',
        marginBottom: 16,
        gap: 12,
        paddingHorizontal: 4,
    },
    modeButton: {
        flex: 1,
        paddingVertical: 16,
        paddingHorizontal: 12,
        borderRadius: 20,
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderWidth: 2,
        borderColor: '#e2e8f0',
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.08,
        shadowRadius: 8,
        elevation: 3,
    },
    modeButtonActive: {
        backgroundColor: '#2563eb',
        borderColor: '#2563eb',
        shadowColor: '#2563eb',
        shadowOpacity: 0.4,
        shadowRadius: 12,
        elevation: 6,
    },
    modeButtonText: {
        fontSize: 15,
        fontWeight: '600',
        color: '#64748b',
        textAlign: 'center',
        lineHeight: 20,
    },
    modeButtonTextActive: {
        color: '#ffffff',
    },
    contentCard: {
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 28,
        marginBottom: 20,
        shadowColor: '#000',
        shadowOffset: {width: 0, height: 15},
        shadowOpacity: 0.1,
        shadowRadius: 30,
        elevation: 8,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.5)',
    },
    loadingContainer: {
        alignItems: 'center',
        padding: 30,
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 28,
        marginBottom: 20,
        shadowColor: '#000',
        shadowOffset: {width: 0, height: 15},
        shadowOpacity: 0.1,
        shadowRadius: 30,
        elevation: 8,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.5)',
    },
    loadingText: {
        marginTop: 12,
        fontSize: 16,
        color: '#64748b',
        fontWeight: '500',
    },
    resultCard: {
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 28,
        padding: 24,
        marginBottom: 20,
        shadowColor: '#000',
        shadowOffset: {width: 0, height: 15},
        shadowOpacity: 0.1,
        shadowRadius: 30,
        elevation: 8,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.5)',
    },
    resultTitle: {
        fontSize: 22,
        fontWeight: 'bold',
        color: '#1e293b',
        marginBottom: 20,
        textAlign: 'center',
    },
    resultContent: {
        alignItems: 'center',
    },
    digitDisplay: {
        width: 140,
        height: 140,
        backgroundColor: '#2563eb',
        borderRadius: 20,
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 20,
        shadowColor: '#2563eb',
        shadowOffset: {width: 0, height: 8},
        shadowOpacity: 0.3,
        shadowRadius: 16,
        elevation: 8,
    },
    resultDigit: {
        fontSize: 72,
        fontWeight: 'bold',
        color: '#fff',
        textShadowColor: 'rgba(0, 0, 0, 0.2)',
        textShadowOffset: { width: 0, height: 2 },
        textShadowRadius: 4,
    },
    resultSequence: {
        fontSize: 52,
        fontWeight: 'bold',
        color: '#2563eb',
        letterSpacing: 6,
        marginBottom: 15,
        textShadowColor: 'rgba(37, 99, 235, 0.2)',
        textShadowOffset: { width: 0, height: 2 },
        textShadowRadius: 4,
    },
    multiResultDetails: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        justifyContent: 'center',
        marginTop: 20,
        gap: 12,
    },
    digitResultCard: {
        backgroundColor: 'rgba(37, 99, 235, 0.1)',
        borderRadius: 16,
        padding: 16,
        minWidth: 70,
        alignItems: 'center',
        borderWidth: 2,
        borderColor: '#2563eb',
    },
    digitResultNumber: {
        fontSize: 32,
        fontWeight: 'bold',
        color: '#2563eb',
    },
    digitResultConf: {
        fontSize: 13,
        color: '#10b981',
        marginTop: 6,
        fontWeight: '600',
    },
    resultSubtext: {
        fontSize: 16,
        color: '#64748b',
        textAlign: 'center',
        fontWeight: '500',
    },
    headerRow: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    textColumn: {
        marginTop: 13,
        marginLeft: 12,
    }
});

export default MainScreen;