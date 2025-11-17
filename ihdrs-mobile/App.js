import React, { useState } from 'react';
import { StatusBar } from 'expo-status-bar';
import { View, StyleSheet } from 'react-native';
import LoginScreen from './src/screens/LoginScreen';
import MainScreen from './src/screens/MainScreen';

export default function App() {
    const [user, setUser] = useState(null);         // null = 未登录
    const [showLogin, setShowLogin] = useState(false); // 控制是否显示登录页面

    const handleLoginSuccess = (userData) => {
        setUser(userData);
        setShowLogin(false); // 关闭登录页返回主界面
    };

    const handleCancelLogin = () => {
        setShowLogin(false);  // 返回主页面
    };

    const handleLogout = () => {
        setUser(null);       // 清空用户
    };

    // 当 showLogin === true 时显示 LoginScreen
    if (showLogin) {
        return (
            <View style={styles.container}>
                <StatusBar style="light" />
                <LoginScreen onLoginSuccess={handleLoginSuccess}
                             onCancel={handleCancelLogin}/>
            </View>
        );
    }

    // 默认显示 MainScreen
    return (
        <View style={styles.container}>
            <StatusBar style="light" />
            <MainScreen
                user={user}                  // 传递用户
                onLogout={handleLogout}      // 传递退出函数
                onLogin={() => setShowLogin(true)} // 点击右上角登录触发
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
});
