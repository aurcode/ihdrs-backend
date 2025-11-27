import React, { useState } from 'react';
import { StatusBar } from 'expo-status-bar';
import { View, StyleSheet } from 'react-native';
import LoginScreen from './src/screens/LoginScreen';
import RegisterScreen from './src/screens/RegisterScreen';
import ProfileScreen from './src/screens/ProfileScreen';
import HistoryScreen from './src/screens/HistoryScreen';
import FeedbackScreen from './src/screens/FeedbackScreen';
import MainScreen from './src/screens/MainScreen';

export default function App() {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(null);
    const [currentScreen, setCurrentScreen] = useState('main'); // 'main', 'login', 'register', 'profile', 'history', 'feedback'
    const [initialCredentials, setInitialCredentials] = useState(null);

    const handleLoginSuccess = (userData) => {
        setUser(userData.user || userData);
        setToken(userData.token);
        setInitialCredentials(null);
        setCurrentScreen('main');
    };

    const handleRegisterSuccess = () => {
        setCurrentScreen('login');
    };

    const handleNavigateToLogin = (credentials) => {
        setInitialCredentials(credentials || null);
        setCurrentScreen('login');
    };

    const handleNavigateToRegister = () => {
        setCurrentScreen('register');
    };

    const handleNavigateToProfile = () => {
        setCurrentScreen('profile');
    };

    const handleNavigateToHistory = () => {
        setCurrentScreen('history');
    };

    const handleNavigateToFeedback = () => {
        setCurrentScreen('feedback');
    };

    const handleCancelAuth = () => {
        setCurrentScreen('main');
    };

    const handleLogout = () => {
        setUser(null);
        setToken(null);
        setCurrentScreen('main');
    };

    const handleProfileUpdated = () => {
        console.log('Profile updated');
    };

    // 显示反馈页面
    if (currentScreen === 'feedback') {
        return (
            <View style={styles.container}>
                <StatusBar style="light" />
                <FeedbackScreen
                    user={user}
                    token={token}
                    onCancel={handleCancelAuth}
                />
            </View>
        );
    }

    // 显示历史记录页面
    if (currentScreen === 'history') {
        return (
            <View style={styles.container}>
                <StatusBar style="light" />
                <HistoryScreen
                    user={user}
                    token={token}
                    onCancel={handleCancelAuth}
                />
            </View>
        );
    }

    // 显示个人中心页面
    if (currentScreen === 'profile') {
        return (
            <View style={styles.container}>
                <StatusBar style="light" />
                <ProfileScreen
                    user={user}
                    token={token}
                    onProfileUpdated={handleProfileUpdated}
                    onCancel={handleCancelAuth}
                />
            </View>
        );
    }

    // 显示注册页面
    if (currentScreen === 'register') {
        return (
            <View style={styles.container}>
                <StatusBar style="light" />
                <RegisterScreen
                    onRegisterSuccess={handleRegisterSuccess}
                    onNavigateToLogin={handleNavigateToLogin}
                    onCancel={handleCancelAuth}
                />
            </View>
        );
    }

    // 显示登录页面
    if (currentScreen === 'login') {
        return (
            <View style={styles.container}>
                <StatusBar style="light" />
                <LoginScreen
                    onLoginSuccess={handleLoginSuccess}
                    onNavigateToRegister={handleNavigateToRegister}
                    onCancel={handleCancelAuth}
                    initialCredentials={initialCredentials}
                />
            </View>
        );
    }

    // 默认显示主页面
    return (
        <View style={styles.container}>
            <StatusBar style="light" />
            <MainScreen
                user={user}
                onLogout={handleLogout}
                onLogin={handleNavigateToLogin}
                onRegister={handleNavigateToRegister}
                onProfile={handleNavigateToProfile}
                onHistory={handleNavigateToHistory}
                onFeedback={handleNavigateToFeedback}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
});