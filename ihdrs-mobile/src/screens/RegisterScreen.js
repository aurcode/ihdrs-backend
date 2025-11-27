// RegisterScreen.js
import React, { useState, useRef, useEffect } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    StyleSheet,
    Alert,
    Platform,
    StatusBar,
    Dimensions,
    Animated,
    Easing,
    ActivityIndicator,
} from 'react-native';
import { KeyboardAwareScrollView } from 'react-native-keyboard-aware-scroll-view';
import authService from '../services/authService';

const { width, height } = Dimensions.get('window');

const RegisterScreen = ({ onRegisterSuccess, onNavigateToLogin, onCancel }) => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [email, setEmail] = useState('');
    const [phone, setPhone] = useState('');
    const [loading, setLoading] = useState(false);

    // 输入框聚焦状态
    const [usernameFocused, setUsernameFocused] = useState(false);
    const [passwordFocused, setPasswordFocused] = useState(false);
    const [confirmPasswordFocused, setConfirmPasswordFocused] = useState(false);
    const [emailFocused, setEmailFocused] = useState(false);
    const [phoneFocused, setPhoneFocused] = useState(false);

    // 密码一致性状态
    const [passwordMatch, setPasswordMatch] = useState(null); // null: 未检查, true: 匹配, false: 不匹配

    // 动画引用
    const fadeAnim = useRef(new Animated.Value(0)).current;
    const scaleAnim = useRef(new Animated.Value(1)).current;
    const backgroundY = useRef(new Animated.Value(0)).current;
    const passwordMatchAnim = useRef(new Animated.Value(0)).current;

    // 粒子效果
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

    // 实时检查密码一致性
    useEffect(() => {
        if (confirmPassword.length > 0) {
            const match = password === confirmPassword;
            setPasswordMatch(match);

            // 添加动画效果
            Animated.sequence([
                Animated.timing(passwordMatchAnim, {
                    toValue: match ? 1 : 0.5,
                    duration: 200,
                    useNativeDriver: true,
                }),
            ]).start();
        } else {
            setPasswordMatch(null);
            Animated.timing(passwordMatchAnim, {
                toValue: 0,
                duration: 200,
                useNativeDriver: true,
            }).start();
        }
    }, [password, confirmPassword]);

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

    const handleInputFocus = (inputType) => {
        switch(inputType) {
            case 'username':
                setUsernameFocused(true);
                break;
            case 'password':
                setPasswordFocused(true);
                break;
            case 'confirmPassword':
                setConfirmPasswordFocused(true);
                break;
            case 'email':
                setEmailFocused(true);
                break;
            case 'phone':
                setPhoneFocused(true);
                break;
        }
    };

    const handleInputBlur = (inputType) => {
        switch(inputType) {
            case 'username':
                setUsernameFocused(false);
                break;
            case 'password':
                setPasswordFocused(false);
                break;
            case 'confirmPassword':
                setConfirmPasswordFocused(false);
                break;
            case 'email':
                setEmailFocused(false);
                break;
            case 'phone':
                setPhoneFocused(false);
                break;
        }
    };

    // 验证
    const validateUsername = (username) => {
        if (username.length < 3 || username.length > 50)
            return '用户名长度必须在 3-50 个字符之间';
        if (!/^[a-zA-Z0-9_]+$/.test(username))
            return '用户名只能包含字母、数字和下划线';
        return null;
    };

    const validatePassword = (password) => {
        if (password.length < 6 || password.length > 20)
            return '密码长度必须在 6-20 个字符之间';
        return null;
    };

    const validateEmail = (email) => {
        if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
            return '邮箱格式不正确';
        return null;
    };

    const validatePhone = (phone) => {
        if (phone && !/^1[3-9]\d{9}$/.test(phone))
            return '手机号格式不正确';
        return null;
    };

    const handleRegister = async () => {
        if (!username || !password || !confirmPassword) {
            // 错误动画
            Animated.sequence([
                Animated.timing(scaleAnim, {
                    toValue: 0.95,
                    duration: 100,
                    useNativeDriver: true,
                }),
                Animated.timing(scaleAnim, {
                    toValue: 1,
                    duration: 100,
                    useNativeDriver: true,
                }),
            ]).start();

            Alert.alert('提示', '请填写所有必填字段');
            return;
        }

        const usernameError = validateUsername(username);
        const passwordError = validatePassword(password);

        if (usernameError) {
            Alert.alert('验证失败', usernameError);
            return;
        }

        if (passwordError) {
            Alert.alert('验证失败', passwordError);
            return;
        }

        if (password !== confirmPassword) {
            Alert.alert('验证失败', '两次输入的密码不一致');
            return;
        }

        if (email) {
            const emailError = validateEmail(email);
            if (emailError) {
                Alert.alert('验证失败', emailError);
                return;
            }
        }

        if (phone) {
            const phoneError = validatePhone(phone);
            if (phoneError) {
                Alert.alert('验证失败', phoneError);
                return;
            }
        }

        setLoading(true);

        try {
            const res = await authService.register(username, password, email || null, phone || null);

            if (res.success) {
                // 成功动画
                Animated.sequence([
                    Animated.timing(scaleAnim, {
                        toValue: 1.05,
                        duration: 200,
                        useNativeDriver: true,
                    }),
                    Animated.timing(scaleAnim, {
                        toValue: 1,
                        duration: 200,
                        useNativeDriver: true,
                    }),
                ]).start();

                Alert.alert('注册成功', '您的账户已创建成功！', [
                    { text: '去登录', onPress: () => onNavigateToLogin({ username, password }) },
                ]);
            } else {
                // 失败动画
                Animated.sequence([
                    Animated.timing(scaleAnim, {
                        toValue: 0.98,
                        duration: 100,
                        useNativeDriver: true,
                    }),
                    Animated.timing(scaleAnim, {
                        toValue: 1.02,
                        duration: 100,
                        useNativeDriver: true,
                    }),
                    Animated.timing(scaleAnim, {
                        toValue: 1,
                        duration: 100,
                        useNativeDriver: true,
                    }),
                ]).start();

                Alert.alert('注册失败', res.error || '注册失败，请重试');
            }
        } catch (err) {
            // 错误动画
            Animated.sequence([
                Animated.timing(scaleAnim, {
                    toValue: 0.98,
                    duration: 100,
                    useNativeDriver: true,
                }),
                Animated.timing(scaleAnim, {
                    toValue: 1.02,
                    duration: 100,
                    useNativeDriver: true,
                }),
                Animated.timing(scaleAnim, {
                    toValue: 1,
                    duration: 100,
                    useNativeDriver: true,
                }),
            ]).start();

            Alert.alert('错误', '注册失败，请重试');
        } finally {
            setLoading(false);
        }
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

            {/* 返回按钮 - 固定在左上角 */}
            <TouchableOpacity
                onPress={onCancel}
                style={styles.backButton}
                disabled={loading}
                activeOpacity={0.8}
            >
                <Text style={styles.backButtonText}>← 返回</Text>
            </TouchableOpacity>

            <KeyboardAwareScrollView
                enableOnAndroid={true}
                extraScrollHeight={Platform.OS === 'android' ? 20 : 10}
                keyboardShouldPersistTaps="handled"
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                <Animated.View
                    style={[
                        styles.contentContainer,
                        { transform: [{ scale: scaleAnim }] }
                    ]}
                >
                    {/* 注册卡片 */}
                    <Animated.View
                        style={[
                            styles.card,
                            {
                                opacity: fadeAnim.interpolate({
                                    inputRange: [0, 0.6, 1],
                                    outputRange: [0, 0, 1]
                                }),
                                transform: [{
                                    translateY: fadeAnim.interpolate({
                                        inputRange: [0, 0.6, 1],
                                        outputRange: [50, 50, 0]
                                    })
                                }]
                            }
                        ]}
                    >
                        <Text style={styles.welcomeText}>创建新账户</Text>
                        <Text style={styles.welcomeSubtext}>填写以下信息完成注册</Text>

                        {/* USERNAME */}
                        <View style={styles.inputContainer}>
                            <Text style={[
                                styles.label,
                                usernameFocused && styles.labelFocused
                            ]}>
                                用户名 <Text style={styles.required}>*</Text>
                            </Text>
                            <Animated.View
                                style={[
                                    styles.inputWrapper,
                                    {
                                        borderColor: usernameFocused ? '#2563eb' : '#e2e8f0',
                                        backgroundColor: usernameFocused ? '#eff6ff' : '#f8fafc',
                                        shadowColor: usernameFocused ? '#2563eb' : 'transparent',
                                        shadowOpacity: usernameFocused ? 0.2 : 0,
                                    }
                                ]}
                            >
                                <Text style={[
                                    styles.inputIcon,
                                    usernameFocused && styles.inputIconFocused
                                ]}>👤</Text>
                                <TextInput
                                    style={styles.input}
                                    placeholder="3-50字符，仅字母数字下划线"
                                    placeholderTextColor="#94a3b8"
                                    value={username}
                                    onChangeText={setUsername}
                                    autoCapitalize="none"
                                    returnKeyType="next"
                                    onFocus={() => handleInputFocus('username')}
                                    onBlur={() => handleInputBlur('username')}
                                    selectionColor="#2563eb"
                                />
                            </Animated.View>
                        </View>

                        {/* PASSWORD */}
                        <View style={styles.inputContainer}>
                            <Text style={[
                                styles.label,
                                passwordFocused && styles.labelFocused
                            ]}>
                                密码 <Text style={styles.required}>*</Text>
                            </Text>
                            <Animated.View
                                style={[
                                    styles.inputWrapper,
                                    {
                                        borderColor: passwordFocused ? '#2563eb' : '#e2e8f0',
                                        backgroundColor: passwordFocused ? '#eff6ff' : '#f8fafc',
                                        shadowColor: passwordFocused ? '#2563eb' : 'transparent',
                                        shadowOpacity: passwordFocused ? 0.2 : 0,
                                    }
                                ]}
                            >
                                <Text style={[
                                    styles.inputIcon,
                                    passwordFocused && styles.inputIconFocused
                                ]}>🔒</Text>
                                <TextInput
                                    style={styles.input}
                                    placeholder="6-20字符"
                                    placeholderTextColor="#94a3b8"
                                    value={password}
                                    onChangeText={setPassword}
                                    secureTextEntry
                                    returnKeyType="next"
                                    onFocus={() => handleInputFocus('password')}
                                    onBlur={() => handleInputBlur('password')}
                                    selectionColor="#2563eb"
                                />
                            </Animated.View>
                        </View>

                        {/* CONFIRM PASSWORD */}
                        <View style={styles.inputContainer}>
                            <Text style={[
                                styles.label,
                                confirmPasswordFocused && styles.labelFocused,
                                passwordMatch === false && styles.labelError,
                                passwordMatch === true && styles.labelSuccess
                            ]}>
                                确认密码 <Text style={styles.required}>*</Text>
                            </Text>
                            <Animated.View
                                style={[
                                    styles.inputWrapper,
                                    {
                                        borderColor: passwordMatch === true
                                            ? '#10b981'
                                            : passwordMatch === false
                                                ? '#ef4444'
                                                : confirmPasswordFocused
                                                    ? '#2563eb'
                                                    : '#e2e8f0',
                                        backgroundColor: passwordMatch === true
                                            ? '#f0fdf4'
                                            : passwordMatch === false
                                                ? '#fef2f2'
                                                : confirmPasswordFocused
                                                    ? '#eff6ff'
                                                    : '#f8fafc',
                                        shadowColor: passwordMatch === true
                                            ? '#10b981'
                                            : passwordMatch === false
                                                ? '#ef4444'
                                                : confirmPasswordFocused
                                                    ? '#2563eb'
                                                    : 'transparent',
                                        shadowOpacity: (passwordMatch !== null || confirmPasswordFocused) ? 0.2 : 0,
                                    }
                                ]}
                            >
                                <Text style={[
                                    styles.inputIcon,
                                    confirmPasswordFocused && styles.inputIconFocused,
                                    passwordMatch === true && styles.inputIconSuccess,
                                    passwordMatch === false && styles.inputIconError
                                ]}>
                                    {passwordMatch === true ? '✅' : passwordMatch === false ? '❌' : '🔐'}
                                </Text>
                                <TextInput
                                    style={styles.input}
                                    placeholder="再次输入密码"
                                    placeholderTextColor="#94a3b8"
                                    value={confirmPassword}
                                    onChangeText={setConfirmPassword}
                                    secureTextEntry
                                    onFocus={() => handleInputFocus('confirmPassword')}
                                    onBlur={() => handleInputBlur('confirmPassword')}
                                    selectionColor={passwordMatch === true ? '#10b981' : passwordMatch === false ? '#ef4444' : '#2563eb'}
                                />
                            </Animated.View>
                            {/* 密码一致性提示 */}
                            {confirmPassword.length > 0 && passwordMatch !== null && (
                                <Animated.View
                                    style={[
                                        styles.passwordMatchHint,
                                        {
                                            opacity: passwordMatchAnim,
                                            transform: [{
                                                translateY: passwordMatchAnim.interpolate({
                                                    inputRange: [0, 1],
                                                    outputRange: [-10, 0]
                                                })
                                            }]
                                        }
                                    ]}
                                >
                                    <Text style={[
                                        styles.passwordMatchText,
                                        passwordMatch ? styles.passwordMatchTextSuccess : styles.passwordMatchTextError
                                    ]}>
                                        {passwordMatch ? '密码匹配' : '密码不匹配'}
                                    </Text>
                                </Animated.View>
                            )}
                        </View>

                        {/* EMAIL */}
                        <View style={styles.inputContainer}>
                            <Text style={[
                                styles.label,
                                emailFocused && styles.labelFocused
                            ]}>
                                邮箱 (可选)
                            </Text>
                            <Animated.View
                                style={[
                                    styles.inputWrapper,
                                    {
                                        borderColor: emailFocused ? '#2563eb' : '#e2e8f0',
                                        backgroundColor: emailFocused ? '#eff6ff' : '#f8fafc',
                                        shadowColor: emailFocused ? '#2563eb' : 'transparent',
                                        shadowOpacity: emailFocused ? 0.2 : 0,
                                    }
                                ]}
                            >
                                <Text style={[
                                    styles.inputIcon,
                                    emailFocused && styles.inputIconFocused
                                ]}>📧</Text>
                                <TextInput
                                    style={styles.input}
                                    placeholder="example@email.com"
                                    placeholderTextColor="#94a3b8"
                                    value={email}
                                    onChangeText={setEmail}
                                    keyboardType="email-address"
                                    autoCapitalize="none"
                                    onFocus={() => handleInputFocus('email')}
                                    onBlur={() => handleInputBlur('email')}
                                    selectionColor="#2563eb"
                                />
                            </Animated.View>
                        </View>

                        {/* PHONE */}
                        <View style={styles.inputContainer}>
                            <Text style={[
                                styles.label,
                                phoneFocused && styles.labelFocused
                            ]}>
                                手机号 (可选)
                            </Text>
                            <Animated.View
                                style={[
                                    styles.inputWrapper,
                                    {
                                        borderColor: phoneFocused ? '#2563eb' : '#e2e8f0',
                                        backgroundColor: phoneFocused ? '#eff6ff' : '#f8fafc',
                                        shadowColor: phoneFocused ? '#2563eb' : 'transparent',
                                        shadowOpacity: phoneFocused ? 0.2 : 0,
                                    }
                                ]}
                            >
                                <Text style={[
                                    styles.inputIcon,
                                    phoneFocused && styles.inputIconFocused
                                ]}>📱</Text>
                                <TextInput
                                    style={styles.input}
                                    placeholder="13800138000"
                                    placeholderTextColor="#94a3b8"
                                    value={phone}
                                    onChangeText={setPhone}
                                    keyboardType="phone-pad"
                                    onFocus={() => handleInputFocus('phone')}
                                    onBlur={() => handleInputBlur('phone')}
                                    selectionColor="#2563eb"
                                />
                            </Animated.View>
                        </View>

                        {/* REGISTER BUTTON */}
                        <TouchableOpacity
                            style={[
                                styles.registerButton,
                                loading && styles.registerButtonDisabled,
                                {
                                    backgroundColor: loading ? '#93c5fd' : '#2563eb',
                                    shadowColor: loading ? '#93c5fd' : '#1d4ed8',
                                }
                            ]}
                            onPress={handleRegister}
                            disabled={loading}
                            activeOpacity={0.8}
                        >
                            {loading ? (
                                <ActivityIndicator color="#ffffff" size="small" />
                            ) : (
                                <Text style={styles.registerButtonText}>注册</Text>
                            )}
                        </TouchableOpacity>

                        {/* LOGIN LINK */}
                        <Animated.View
                            style={[
                                styles.loginLinkContainer,
                                {
                                    opacity: fadeAnim.interpolate({
                                        inputRange: [0, 0.9, 1],
                                        outputRange: [0, 0, 1]
                                    })
                                }
                            ]}
                        >
                            <Text style={styles.loginLinkText}>已有账户？</Text>
                            <TouchableOpacity
                                onPress={onNavigateToLogin}
                                disabled={loading}
                            >
                                <Text style={styles.loginLink}>立即登录</Text>
                            </TouchableOpacity>
                        </Animated.View>
                    </Animated.View>
                </Animated.View>
            </KeyboardAwareScrollView>
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
        background: 'linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%)',
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
    scrollContent: {
        flexGrow: 1,
        minHeight: height,
        padding: 20,
        paddingTop: Platform.OS === 'ios' ? 80 : 60,
        paddingBottom: 40,
    },
    contentContainer: {
        width: '100%',
        maxWidth: 440,
        alignSelf: 'center',
    },
    header: {
        alignItems: 'center',
        marginBottom: 40,
    },
    logoContainer: {
        marginBottom: 25,
    },
    logoCircle: {
        width: 100,
        height: 100,
        borderRadius: 50,
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        justifyContent: 'center',
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.1,
        shadowRadius: 20,
        elevation: 5,
        borderWidth: 3,
        borderColor: 'rgba(59, 130, 246, 0.2)',
    },
    logoText: {
        fontSize: 50,
    },
    title: {
        fontSize: 36,
        fontWeight: 'bold',
        color: '#1e293b',
        marginBottom: 10,
        textAlign: 'center',
        textShadowColor: 'rgba(0, 0, 0, 0.1)',
        textShadowOffset: { width: 0, height: 2 },
        textShadowRadius: 4,
    },
    subtitle: {
        fontSize: 18,
        color: '#475569',
        textAlign: 'center',
        fontWeight: '500',
    },
    card: {
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 28,
        padding: 25,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 15 },
        shadowOpacity: 0.1,
        shadowRadius: 30,
        elevation: 8,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.5)',
        backdropFilter: 'blur(20px)',
    },
    welcomeText: {
        fontSize: 26,
        fontWeight: '700',
        color: '#1e293b',
        marginBottom: 8,
        textAlign: 'center',
    },
    welcomeSubtext: {
        fontSize: 16,
        color: '#64748b',
        marginBottom: 25,
        textAlign: 'center',
    },
    inputContainer: {
        marginBottom: 20,
    },
    label: {
        fontSize: 16,
        fontWeight: '600',
        color: '#334155',
        marginBottom: 10,
        transition: 'color 0.3s ease',
    },
    labelFocused: {
        color: '#2563eb',
        fontWeight: '700',
    },
    required: {
        color: '#ef4444',
        fontWeight: '700',
    },
    inputWrapper: {
        flexDirection: 'row',
        alignItems: 'center',
        borderRadius: 16,
        borderWidth: 2,
        paddingHorizontal: 16,
        height: 60,
        transition: 'all 0.3s ease',
        shadowOffset: { width: 0, height: 4 },
        shadowRadius: 10,
        elevation: 2,
    },
    inputIcon: {
        fontSize: 20,
        marginRight: 12,
        color: '#94a3b8',
        transition: 'color 0.3s ease',
    },
    inputIconFocused: {
        color: '#2563eb',
        transform: [{ scale: 1.1 }],
    },
    inputIconSuccess: {
        color: '#10b981',
    },
    inputIconError: {
        color: '#ef4444',
    },
    labelSuccess: {
        color: '#10b981',
    },
    labelError: {
        color: '#ef4444',
    },
    passwordMatchHint: {
        marginTop: 8,
        marginLeft: 4,
    },
    passwordMatchText: {
        fontSize: 14,
        fontWeight: '600',
    },
    passwordMatchTextSuccess: {
        color: '#10b981',
    },
    passwordMatchTextError: {
        color: '#ef4444',
    },
    input: {
        flex: 1,
        fontSize: 18,
        color: '#1e293b',
        padding: 0,
        fontWeight: '500',
    },
    registerButton: {
        borderRadius: 16,
        height: 60,
        justifyContent: 'center',
        alignItems: 'center',
        marginTop: 15,
        shadowOffset: { width: 0, height: 8 },
        shadowOpacity: 0.2,
        shadowRadius: 20,
        elevation: 5,
        transition: 'all 0.3s ease',
    },
    registerButtonDisabled: {
        opacity: 0.7,
        transform: [{ scale: 0.98 }],
    },
    registerButtonText: {
        color: '#fff',
        fontSize: 18,
        fontWeight: '700',
        textShadowColor: 'rgba(0, 0, 0, 0.2)',
        textShadowOffset: { width: 0, height: 1 },
        textShadowRadius: 2,
    },
    loginLinkContainer: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        marginTop: 25,
    },
    loginLinkText: {
        fontSize: 16,
        color: '#64748b',
        marginRight: 8,
        fontWeight: '500',
    },
    loginLink: {
        fontSize: 16,
        color: '#2563eb',
        fontWeight: '700',
        textDecorationLine: 'underline',
        textDecorationColor: '#2563eb',
        textDecorationThickness: 2,
        textUnderlineOffset: 4,
    },
    backButton: {
        position: 'absolute',
        top: Platform.OS === 'ios' ? 50 : 30,
        left: 20,
        zIndex: 1000,
        padding: 12,
        borderRadius: 25,
        backgroundColor: 'rgba(255, 255, 255, 0.7)',
        backdropFilter: 'blur(10px)',
        borderWidth: 1,
        borderColor: 'rgba(59, 130, 246, 0.2)',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 5,
    },
    backButtonText: {
        color: '#1e293b',
        fontSize: 16,
        fontWeight: '600',
        textShadowColor: 'rgba(0, 0, 0, 0.1)',
        textShadowOffset: { width: 0, height: 1 },
        textShadowRadius: 2,
    },
});

export default RegisterScreen;