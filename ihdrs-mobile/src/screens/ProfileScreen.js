import React, { useState, useEffect } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    StyleSheet,
    Alert,
    KeyboardAvoidingView,
    Platform,
    ScrollView,
    Animated,
    Dimensions,
    StatusBar,
    ActivityIndicator,
} from 'react-native';
import userService from '../services/userService';

const { width } = Dimensions.get('window');

const ProfileScreen = ({ user, token, onProfileUpdated, onCancel }) => {
    // Profile data states
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [profile, setProfile] = useState(null);

    // Form states
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [phone, setPhone] = useState('');
    const [focusedInput, setFocusedInput] = useState(null);

    // Password change states
    const [showPasswordForm, setShowPasswordForm] = useState(false);
    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmNewPassword, setConfirmNewPassword] = useState('');

    // Animation states
    const [fadeAnim] = useState(new Animated.Value(0));
    const [slideAnim] = useState(new Animated.Value(50));

    useEffect(() => {
        loadUserProfile();

        Animated.parallel([
            Animated.timing(fadeAnim, {
                toValue: 1,
                duration: 800,
                useNativeDriver: true,
            }),
            Animated.timing(slideAnim, {
                toValue: 0,
                duration: 600,
                useNativeDriver: true,
            }),
        ]).start();
    }, []);

    const loadUserProfile = async () => {
        try {
            setLoading(true);
            const response = await userService.getCurrentUser(token);

            if (response.success) {
                setProfile(response.data);
                setUsername(response.data.username || '');
                setEmail(response.data.email || '');
                setPhone(response.data.phone || '');
            } else {
                Alert.alert('错误', response.error || '加载用户信息失败');
            }
        } catch (error) {
            Alert.alert('错误', '加载用户信息失败');
        } finally {
            setLoading(false);
        }
    };

    // Validation functions
    const validateUsername = (username) => {
        if (username.length < 3 || username.length > 20) {
            return '用户名长度必须在 3-20 个字符之间';
        }
        if (!/^[a-zA-Z0-9_]+$/.test(username)) {
            return '用户名只能包含字母、数字和下划线';
        }
        return null;
    };

    const validateEmail = (email) => {
        if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            return '邮箱格式不正确';
        }
        return null;
    };

    const validatePhone = (phone) => {
        if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
            return '手机号格式不正确';
        }
        return null;
    };

    const validatePassword = (password) => {
        if (password.length < 6 || password.length > 20) {
            return '密码长度必须在 6-20 个字符之间';
        }
        return null;
    };

    const handleUpdateProfile = async () => {
        // Validate username
        if (!username) {
            Alert.alert('验证失败', '用户名不能为空');
            return;
        }

        const usernameError = validateUsername(username);
        if (usernameError) {
            Alert.alert('验证失败', usernameError);
            return;
        }

        // Check if username changed and already exists
        if (username !== profile.username) {
            const checkResult = await userService.checkUsername(token, username);
            if (checkResult.success && checkResult.exists) {
                Alert.alert('验证失败', '用户名已被占用');
                return;
            }
        }

        // Validate email if provided
        if (email) {
            const emailError = validateEmail(email);
            if (emailError) {
                Alert.alert('验证失败', emailError);
                return;
            }
        }

        // Validate phone if provided
        if (phone) {
            const phoneError = validatePhone(phone);
            if (phoneError) {
                Alert.alert('验证失败', phoneError);
                return;
            }
        }

        setSaving(true);
        try {
            const response = await userService.updateProfile(token, {
                username: username.trim(),
                email: email.trim() || null,
                telephone: phone.trim() || null,
            });

            if (response.success) {
                Alert.alert('成功', '个人信息更新成功', [
                    {
                        text: '确定',
                        onPress: () => {
                            loadUserProfile();
                            onProfileUpdated && onProfileUpdated();
                        },
                    },
                ]);
            } else {
                Alert.alert('更新失败', response.error || '更新个人信息失败');
            }
        } catch (error) {
            Alert.alert('错误', '更新个人信息失败');
        } finally {
            setSaving(false);
        }
    };

    const handleChangePassword = async () => {
        // Validate old password
        if (!oldPassword) {
            Alert.alert('验证失败', '请输入原密码');
            return;
        }

        // Validate new password
        if (!newPassword) {
            Alert.alert('验证失败', '请输入新密码');
            return;
        }

        const passwordError = validatePassword(newPassword);
        if (passwordError) {
            Alert.alert('验证失败', passwordError);
            return;
        }

        // Check password confirmation
        if (newPassword !== confirmNewPassword) {
            Alert.alert('验证失败', '两次输入的新密码不一致');
            return;
        }

        // Check if new password is same as old
        if (oldPassword === newPassword) {
            Alert.alert('验证失败', '新密码不能与原密码相同');
            return;
        }

        setSaving(true);
        try {
            const response = await userService.changePassword(token, oldPassword, newPassword);

            if (response.success) {
                Alert.alert('成功', '密码修改成功', [
                    {
                        text: '确定',
                        onPress: () => {
                            setShowPasswordForm(false);
                            setOldPassword('');
                            setNewPassword('');
                            setConfirmNewPassword('');
                        },
                    },
                ]);
            } else {
                Alert.alert('修改失败', response.error || '密码修改失败');
            }
        } catch (error) {
            Alert.alert('错误', '密码修改失败');
        } finally {
            setSaving(false);
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return '未知';
        const date = new Date(dateString);
        return date.toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    if (loading) {
        return (
            <View style={styles.loadingContainer}>
                <StatusBar barStyle="light-content" backgroundColor="#667eea" />
                <ActivityIndicator size="large" color="#667eea" />
                <Text style={styles.loadingText}>加载中...</Text>
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <StatusBar barStyle="light-content" backgroundColor="#667eea" />

            {/* Background Gradient Effect */}
            <View style={styles.backgroundGradient}>
                <View style={styles.circleTop} />
                <View style={styles.circleBottom} />
            </View>

            {/* Back Button - 固定在左上角 */}
            <TouchableOpacity
                onPress={onCancel}
                style={styles.backButton}
                activeOpacity={0.8}
            >
                <Text style={styles.backButtonText}>← 返回</Text>
            </TouchableOpacity>

            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
                style={styles.keyboardView}
            >
                <ScrollView
                    contentContainerStyle={styles.scrollContent}
                    showsVerticalScrollIndicator={false}
                    keyboardShouldPersistTaps="handled"
                >
                    <Animated.View
                        style={[
                            styles.contentContainer,
                            {
                                opacity: fadeAnim,
                                transform: [{ translateY: slideAnim }],
                            },
                        ]}
                    >
                        {/* Header Section */}
                        <View style={styles.header}>
                            <View style={styles.logoContainer}>
                                <View style={styles.logoCircle}>
                                    <Text style={styles.logoText}>👤</Text>
                                </View>
                            </View>
                            <Text style={styles.title}>个人中心</Text>
                            <Text style={styles.subtitle}>Personal Profile</Text>
                        </View>

                        {/* User Info Card */}
                        <View style={styles.card}>
                            <View style={styles.cardHeader}>
                                <Text style={styles.cardTitle}>基本信息</Text>
                                <View style={styles.roleBadge}>
                                    <Text style={styles.roleText}>
                                        {profile?.role === 'ADMIN' ? '管理员' : '普通用户'}
                                    </Text>
                                </View>
                            </View>

                            {/* Username Input */}
                            <View style={styles.inputContainer}>
                                <Text style={styles.label}>
                                    用户名 <Text style={styles.required}>*</Text>
                                </Text>
                                <View
                                    style={[
                                        styles.inputWrapper,
                                        focusedInput === 'username',
                                    ]}
                                >
                                    <Text style={styles.inputIcon}>👤</Text>
                                    <TextInput
                                        style={styles.input}
                                        placeholder="请输入用户名"
                                        placeholderTextColor="#94a3b8"
                                        value={username}
                                        onChangeText={setUsername}
                                        autoCapitalize="none"
                                        editable={false}
                                        onFocus={() => setFocusedInput('username')}
                                        onBlur={() => setFocusedInput(null)}
                                    />
                                </View>
                            </View>

                            {/* Email Input */}
                            <View style={styles.inputContainer}>
                                <Text style={styles.label}>
                                    邮箱 <Text style={styles.optional}>(可选)</Text>
                                </Text>
                                <View
                                    style={[
                                        styles.inputWrapper,
                                        focusedInput === 'email' && styles.inputWrapperFocused,
                                    ]}
                                >
                                    <Text style={styles.inputIcon}>📧</Text>
                                    <TextInput
                                        style={styles.input}
                                        placeholder="example@email.com"
                                        placeholderTextColor="#94a3b8"
                                        value={email}
                                        onChangeText={setEmail}
                                        keyboardType="email-address"
                                        autoCapitalize="none"
                                        editable={!saving}
                                        onFocus={() => setFocusedInput('email')}
                                        onBlur={() => setFocusedInput(null)}
                                    />
                                </View>
                            </View>

                            {/* Phone Input */}
                            <View style={styles.inputContainer}>
                                <Text style={styles.label}>
                                    手机号 <Text style={styles.optional}>(可选)</Text>
                                </Text>
                                <View
                                    style={[
                                        styles.inputWrapper,
                                        focusedInput === 'phone' && styles.inputWrapperFocused,
                                    ]}
                                >
                                    <Text style={styles.inputIcon}>📱</Text>
                                    <TextInput
                                        style={styles.input}
                                        placeholder="13800138000"
                                        placeholderTextColor="#94a3b8"
                                        value={phone}
                                        onChangeText={setPhone}
                                        keyboardType="phone-pad"
                                        editable={!saving}
                                        onFocus={() => setFocusedInput('phone')}
                                        onBlur={() => setFocusedInput(null)}
                                    />
                                </View>
                            </View>

                            {/* Update Button */}
                            <TouchableOpacity
                                style={[
                                    styles.updateButton,
                                    saving && styles.updateButtonDisabled,
                                ]}
                                onPress={handleUpdateProfile}
                                disabled={saving}
                                activeOpacity={0.8}
                            >
                                <Text style={styles.updateButtonText}>
                                    {saving ? '保存中...' : '保存修改'}
                                </Text>
                            </TouchableOpacity>

                            {/* Account Info */}
                            <View style={styles.infoContainer}>
                                <View style={styles.infoRow}>
                                    <Text style={styles.infoLabel}>注册时间：</Text>
                                    <Text style={styles.infoValue}>{formatDate(profile?.createTime)}</Text>
                                </View>
                                <View style={styles.infoRow}>
                                    <Text style={styles.infoLabel}>最后登录：</Text>
                                    <Text style={styles.infoValue}>{formatDate(profile?.lastLoginTime)}</Text>
                                </View>
                                <View style={styles.infoRow}>
                                    <Text style={styles.infoLabel}>登录次数：</Text>
                                    <Text style={styles.infoValue}>{profile?.loginCount || 0} 次</Text>
                                </View>
                                <View style={styles.infoRow}>
                                    <Text style={styles.infoLabel}>账户状态：</Text>
                                    <Text style={[styles.infoValue, profile?.status && styles.statusActive]}>
                                        {profile?.status ? '正常' : '已禁用'}
                                    </Text>
                                </View>
                            </View>
                        </View>

                        {/* Password Change Section */}
                        <View style={styles.card}>
                            <TouchableOpacity
                                style={styles.passwordToggle}
                                onPress={() => setShowPasswordForm(!showPasswordForm)}
                                activeOpacity={0.7}
                            >
                                <Text style={styles.cardTitle}>修改密码</Text>
                                <Text style={styles.toggleIcon}>
                                    {showPasswordForm ? '▼' : '▶'}
                                </Text>
                            </TouchableOpacity>

                            {showPasswordForm && (
                                <View style={styles.passwordForm}>
                                    {/* Old Password */}
                                    <View style={styles.inputContainer}>
                                        <Text style={styles.label}>
                                            原密码 <Text style={styles.required}>*</Text>
                                        </Text>
                                        <View
                                            style={[
                                                styles.inputWrapper,
                                                focusedInput === 'oldPassword' && styles.inputWrapperFocused,
                                            ]}
                                        >
                                            <Text style={styles.inputIcon}>🔒</Text>
                                            <TextInput
                                                style={styles.input}
                                                placeholder="请输入原密码"
                                                placeholderTextColor="#94a3b8"
                                                value={oldPassword}
                                                onChangeText={setOldPassword}
                                                secureTextEntry
                                                editable={!saving}
                                                onFocus={() => setFocusedInput('oldPassword')}
                                                onBlur={() => setFocusedInput(null)}
                                            />
                                        </View>
                                    </View>

                                    {/* New Password */}
                                    <View style={styles.inputContainer}>
                                        <Text style={styles.label}>
                                            新密码 <Text style={styles.required}>*</Text>
                                        </Text>
                                        <View
                                            style={[
                                                styles.inputWrapper,
                                                focusedInput === 'newPassword' && styles.inputWrapperFocused,
                                            ]}
                                        >
                                            <Text style={styles.inputIcon}>🔐</Text>
                                            <TextInput
                                                style={styles.input}
                                                placeholder="6-20个字符"
                                                placeholderTextColor="#94a3b8"
                                                value={newPassword}
                                                onChangeText={setNewPassword}
                                                secureTextEntry
                                                editable={!saving}
                                                onFocus={() => setFocusedInput('newPassword')}
                                                onBlur={() => setFocusedInput(null)}
                                            />
                                        </View>
                                    </View>

                                    {/* Confirm New Password */}
                                    <View style={styles.inputContainer}>
                                        <Text style={styles.label}>
                                            确认新密码 <Text style={styles.required}>*</Text>
                                        </Text>
                                        <View
                                            style={[
                                                styles.inputWrapper,
                                                focusedInput === 'confirmNewPassword' && styles.inputWrapperFocused,
                                            ]}
                                        >
                                            <Text style={styles.inputIcon}>🔐</Text>
                                            <TextInput
                                                style={styles.input}
                                                placeholder="再次输入新密码"
                                                placeholderTextColor="#94a3b8"
                                                value={confirmNewPassword}
                                                onChangeText={setConfirmNewPassword}
                                                secureTextEntry
                                                editable={!saving}
                                                onFocus={() => setFocusedInput('confirmNewPassword')}
                                                onBlur={() => setFocusedInput(null)}
                                            />
                                        </View>
                                    </View>

                                    {/* Change Password Button */}
                                    <TouchableOpacity
                                        style={[
                                            styles.changePasswordButton,
                                            saving && styles.changePasswordButtonDisabled,
                                        ]}
                                        onPress={handleChangePassword}
                                        disabled={saving}
                                        activeOpacity={0.8}
                                    >
                                        <Text style={styles.changePasswordButtonText}>
                                            {saving ? '修改中...' : '确认修改'}
                                        </Text>
                                    </TouchableOpacity>
                                </View>
                            )}
                        </View>
                    </Animated.View>
                </ScrollView>
            </KeyboardAvoidingView>
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#667eea',
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#667eea',
    },
    loadingText: {
        marginTop: 12,
        fontSize: 16,
        color: '#ffffff',
    },
    backgroundGradient: {
        position: 'absolute',
        width: '100%',
        height: '100%',
    },
    circleTop: {
        position: 'absolute',
        width: width * 1.5,
        height: width * 1.5,
        borderRadius: width * 0.75,
        backgroundColor: 'rgba(118, 75, 162, 0.3)',
        top: -width * 0.5,
        right: -width * 0.3,
    },
    circleBottom: {
        position: 'absolute',
        width: width * 1.2,
        height: width * 1.2,
        borderRadius: width * 0.6,
        backgroundColor: 'rgba(237, 137, 54, 0.2)',
        bottom: -width * 0.4,
        left: -width * 0.3,
    },
    keyboardView: {
        flex: 1,
    },
    scrollContent: {
        flexGrow: 1,
        justifyContent: 'center',
        padding: 20,
        paddingTop: Platform.OS === 'ios' ? 60 : 40,
        paddingBottom: 40,
    },
    contentContainer: {
        width: '100%',
        maxWidth: 440,
        alignSelf: 'center',
    },
    header: {
        alignItems: 'center',
        marginBottom: 30,
    },
    logoContainer: {
        marginBottom: 20,
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
        shadowOpacity: 0.2,
        shadowRadius: 16,
        elevation: 10,
    },
    logoText: {
        fontSize: 40,
    },
    title: {
        fontSize: 32,
        fontWeight: 'bold',
        color: '#ffffff',
        marginBottom: 8,
        textAlign: 'center',
        textShadowColor: 'rgba(0, 0, 0, 0.1)',
        textShadowOffset: { width: 0, height: 2 },
        textShadowRadius: 4,
    },
    subtitle: {
        fontSize: 16,
        color: 'rgba(255, 255, 255, 0.9)',
        textAlign: 'center',
        letterSpacing: 1,
    },
    card: {
        backgroundColor: '#ffffff',
        borderRadius: 24,
        padding: 28,
        marginBottom: 20,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.25,
        shadowRadius: 20,
        elevation: 15,
    },
    cardHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 24,
    },
    cardTitle: {
        fontSize: 22,
        fontWeight: 'bold',
        color: '#1e293b',
    },
    roleBadge: {
        backgroundColor: '#eff6ff',
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#3b82f6',
    },
    roleText: {
        fontSize: 12,
        color: '#1e40af',
        fontWeight: '600',
    },
    inputContainer: {
        marginBottom: 18,
    },
    label: {
        fontSize: 15,
        fontWeight: '600',
        color: '#334155',
        marginBottom: 10,
    },
    required: {
        color: '#ef4444',
        fontSize: 14,
    },
    optional: {
        color: '#94a3b8',
        fontSize: 13,
        fontWeight: 'normal',
    },
    inputWrapper: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#f8fafc',
        borderRadius: 14,
        borderWidth: 2,
        borderColor: '#e2e8f0',
        paddingHorizontal: 16,
        height: 56,
    },
    inputIcon: {
        fontSize: 20,
        marginRight: 12,
    },
    input: {
        flex: 1,
        fontSize: 16,
        color: '#1e293b',
        padding: 0,
    },
    updateButton: {
        backgroundColor: '#667eea',
        borderRadius: 14,
        height: 56,
        justifyContent: 'center',
        alignItems: 'center',
        marginTop: 12,
        marginBottom: 20,
        shadowColor: '#667eea',
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 0.4,
        shadowRadius: 12,
        elevation: 8,
    },
    updateButtonDisabled: {
        opacity: 0.6,
    },
    updateButtonText: {
        color: '#ffffff',
        fontSize: 18,
        fontWeight: 'bold',
        letterSpacing: 0.5,
    },
    infoContainer: {
        backgroundColor: '#f8fafc',
        borderRadius: 12,
        padding: 16,
        borderWidth: 1,
        borderColor: '#e2e8f0',
    },
    infoRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingVertical: 8,
        borderBottomWidth: 1,
        borderBottomColor: '#e2e8f0',
    },
    infoLabel: {
        fontSize: 14,
        color: '#64748b',
        fontWeight: '500',
    },
    infoValue: {
        fontSize: 14,
        color: '#1e293b',
        fontWeight: '600',
    },
    statusActive: {
        color: '#10b981',
    },
    passwordToggle: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    toggleIcon: {
        fontSize: 18,
        color: '#667eea',
        fontWeight: 'bold',
    },
    passwordForm: {
        marginTop: 24,
    },
    changePasswordButton: {
        backgroundColor: '#f59e0b',
        borderRadius: 14,
        height: 56,
        justifyContent: 'center',
        alignItems: 'center',
        marginTop: 12,
        shadowColor: '#f59e0b',
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 0.4,
        shadowRadius: 12,
        elevation: 8,
    },
    changePasswordButtonDisabled: {
        opacity: 0.6,
    },
    changePasswordButtonText: {
        color: '#ffffff',
        fontSize: 18,
        fontWeight: 'bold',
        letterSpacing: 0.5,
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

export default ProfileScreen;