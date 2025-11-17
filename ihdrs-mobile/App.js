import React, { useState } from 'react';
import { StatusBar } from 'expo-status-bar';
import { View, StyleSheet } from 'react-native';
import LoginScreen from './src/screens/LoginScreen';
import RegisterScreen from './src/screens/RegisterScreen';
import ProfileScreen from './src/screens/ProfileScreen';
import MainScreen from './src/screens/MainScreen';

export default function App() {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(null);
    const [currentScreen, setCurrentScreen] = useState('main'); // 'main', 'login', 'register', 'profile'

    const handleLoginSuccess = (userData) => {
        setUser(userData.user || userData);
        setToken(userData.token);
        setCurrentScreen('main');
    };

    const handleRegisterSuccess = () => {
        setCurrentScreen('login');
    };

    const handleNavigateToLogin = () => {
        setCurrentScreen('login');
    };

    const handleNavigateToRegister = () => {
        setCurrentScreen('register');
    };

    const handleNavigateToProfile = () => {
        setCurrentScreen('profile');
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
        // Reload user data if needed
        console.log('Profile updated');
    };

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
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
});