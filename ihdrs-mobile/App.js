import React, { useState } from 'react';
import { StatusBar } from 'expo-status-bar';
import { View, StyleSheet } from 'react-native';
import LoginScreen from './src/screens/LoginScreen';
import RegisterScreen from './src/screens/RegisterScreen';
import MainScreen from './src/screens/MainScreen';

export default function App() {
    const [user, setUser] = useState(null);         // null = 未登录
    const [currentScreen, setCurrentScreen] = useState('main'); // 'main', 'login', 'register'

    const handleLoginSuccess = (userData) => {
        setUser(userData);
        setCurrentScreen('main'); // 登录成功返回主界面
    };

    const handleRegisterSuccess = () => {
        setCurrentScreen('login'); // 注册成功跳转到登录页
    };

    const handleNavigateToLogin = () => {
        setCurrentScreen('login');
    };

    const handleNavigateToRegister = () => {
        setCurrentScreen('register');
    };

    const handleCancelAuth = () => {
        setCurrentScreen('main');  // 返回主页面
    };

    const handleLogout = () => {
        setUser(null);       // 清空用户
        setCurrentScreen('main');
    };

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
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
});