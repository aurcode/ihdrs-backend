import React, { useState, useEffect } from 'react';
import {
    View,
    Text,
    TouchableOpacity,
    StyleSheet,
    Alert,
    ScrollView,
    Animated,
    Dimensions,
    StatusBar,
    ActivityIndicator,
    Modal,
    TextInput,
    Image,
    Platform,
    KeyboardAvoidingView,
} from 'react-native';
import historyService from '../services/historyService';

const { width } = Dimensions.get('window');

const HistoryScreen = ({ user, token, onCancel }) => {
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [history, setHistory] = useState([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalRecords, setTotalRecords] = useState(0);

    // Filter states
    const [filterResult, setFilterResult] = useState(null);
    const [showFilters, setShowFilters] = useState(false);

    // Feedback modal states
    const [showFeedbackModal, setShowFeedbackModal] = useState(false);
    const [selectedRecord, setSelectedRecord] = useState(null);
    const [correctResult, setCorrectResult] = useState('');
    const [feedbackReason, setFeedbackReason] = useState('');
    const [feedbackType, setFeedbackType] = useState('WRONG_RESULT');
    const [qualityScore, setQualityScore] = useState(3);

    // Selection mode states
    const [selectionMode, setSelectionMode] = useState(false);
    const [selectedIds, setSelectedIds] = useState([]);

    // Animation
    const [fadeAnim] = useState(new Animated.Value(0));
    const [slideAnim] = useState(new Animated.Value(50));

    useEffect(() => {
        loadHistory();

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
    }, [currentPage, filterResult]);

    const loadHistory = async () => {
        try {
            setLoading(true);
            const filters = {};

            if (filterResult !== null) {
                filters.result = filterResult;
            }

            const response = await historyService.getHistory(token, currentPage, 10, filters);

            if (response.success) {
                setHistory(response.data.records || []);
                setTotalPages(response.data.pages || 0);
                setTotalRecords(response.data.total || 0);
            } else {
                Alert.alert('错误', response.error || '加载历史记录失败');
            }
        } catch (error) {
            Alert.alert('错误', '加载历史记录失败');
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    const handleRefresh = () => {
        setRefreshing(true);
        setCurrentPage(0);
        loadHistory();
    };

    const BASE_URL = "http://10.0.2.2:8080";

    const getImageUrl = (path) => {
        if (!path) return null;
        return BASE_URL + path;
    };

    const handleDeleteRecord = (recordId) => {
        Alert.alert(
            '确认删除',
            '确定要删除这条记录吗？',
            [
                { text: '取消', style: 'cancel' },
                {
                    text: '删除',
                    style: 'destructive',
                    onPress: async () => {
                        const response = await historyService.deleteRecord(token, recordId);
                        if (response.success) {
                            Alert.alert('成功', '记录已删除');
                            loadHistory();
                        } else {
                            Alert.alert('删除失败', response.error);
                        }
                    },
                },
            ]
        );
    };

    const handleBatchDelete = () => {
        if (selectedIds.length === 0) {
            Alert.alert('提示', '请至少选择一条记录');
            return;
        }

        Alert.alert(
            '确认删除',
            `确定要删除选中的 ${selectedIds.length} 条记录吗？`,
            [
                { text: '取消', style: 'cancel' },
                {
                    text: '删除',
                    style: 'destructive',
                    onPress: async () => {
                        const response = await historyService.batchDeleteRecords(token, selectedIds);
                        if (response.success) {
                            Alert.alert('成功', '记录已批量删除');
                            setSelectedIds([]);
                            setSelectionMode(false);
                            loadHistory();
                        } else {
                            Alert.alert('删除失败', response.error);
                        }
                    },
                },
            ]
        );
    };

    const handleOpenFeedbackModal = (record) => {
        setSelectedRecord(record);
        setCorrectResult('');
        setFeedbackReason('');
        setFeedbackType('WRONG_RESULT');
        setQualityScore(3);
        setShowFeedbackModal(true);
    };

    const handleSubmitFeedback = async () => {
        if (!correctResult || correctResult === '') {
            Alert.alert('验证失败', '请输入正确的识别结果');
            return;
        }

        const correctResultNum = parseInt(correctResult);
        if (isNaN(correctResultNum)) {
            Alert.alert('验证失败', '识别结果必须是数字');
            return;
        }

        const feedbackData = {
            recordId: selectedRecord.recordId,
            correctResult: correctResultNum,
            feedbackType,
            feedbackReason,
            qualityScore,
        };

        const response = await historyService.submitFeedback(token, feedbackData);

        if (response.success) {
            Alert.alert('成功', '反馈提交成功，感谢您的反馈！');
            setShowFeedbackModal(false);
            loadHistory();
        } else {
            Alert.alert('提交失败', response.error);
        }
    };

    const toggleSelection = (recordId) => {
        if (selectedIds.includes(recordId)) {
            setSelectedIds(selectedIds.filter(id => id !== recordId));
        } else {
            setSelectedIds([...selectedIds, recordId]);
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

    const getConfidenceColor = (confidence) => {
        if (confidence >= 0.9) return '#10b981';
        if (confidence >= 0.7) return '#f59e0b';
        return '#ef4444';
    };

    const renderHistoryItem = (record) => {
        const isSelected = selectedIds.includes(record.recordId);

        return (
            <View
                key={record.recordId}
                style={[
                    styles.historyItem,
                    isSelected && styles.historyItemSelected,
                ]}
            >
                {selectionMode && (
                    <TouchableOpacity
                        style={styles.checkbox}
                        onPress={() => toggleSelection(record.recordId)}
                    >
                        <View style={[styles.checkboxInner, isSelected && styles.checkboxChecked]}>
                            {isSelected && <Text style={styles.checkmark}>✓</Text>}
                        </View>
                    </TouchableOpacity>
                )}

                <View style={styles.historyContent}>
                    <View style={styles.historyHeader}>
                        <View style={styles.resultBadge}>
                            <Text style={styles.resultText}>
                                {record.recognitionResult || "连续"}
                            </Text>
                        </View>
                        <View style={styles.historyInfo}>
                            <Text style={styles.historyDate}>{formatDate(record.createTime)}</Text>
                            <View style={styles.confidenceContainer}>
                                <View
                                    style={[
                                        styles.confidenceDot,
                                        { backgroundColor: getConfidenceColor(record.confidence) },
                                    ]}
                                />
                                <Text style={styles.confidenceText}>
                                    置信度: {(record.confidence * 100).toFixed(1)}%
                                </Text>
                            </View>
                        </View>
                    </View>

                    <View style={styles.historyMeta}>
                        <View style={styles.metaItem}>
                            <Text style={styles.metaLabel}>模型：</Text>
                            <Text style={styles.metaValue}>
                                {record.modelName || 'N/A'} v{record.modelVersion || 'N/A'}
                            </Text>
                        </View>
                        <View style={styles.metaItem}>
                            <Text style={styles.metaLabel}>耗时：</Text>
                            <Text style={styles.metaValue}>{record.processingTime || 0}ms</Text>
                        </View>
                        <View style={styles.metaItem}>
                            <Text style={styles.metaLabel}>输入类型：</Text>
                            <Text style={styles.metaValue}>
                                {record.inputType === 'CANVAS' ? '手绘' : record.inputType === 'UPLOAD' ? '上传' : record.inputType === 'MULTI' ? '手绘' : '相机'}
                            </Text>
                        </View>
                    </View>

                    {!selectionMode && (
                        <View style={styles.historyActions}>
                            <TouchableOpacity
                                style={styles.feedbackButton}
                                onPress={() => handleOpenFeedbackModal(record)}
                                activeOpacity={0.7}
                            >
                                <Text style={styles.feedbackButtonText}>反馈</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.deleteButton}
                                onPress={() => handleDeleteRecord(record.recordId)}
                                activeOpacity={0.7}
                            >
                                <Text style={styles.deleteButtonText}>删除</Text>
                            </TouchableOpacity>
                        </View>
                    )}
                    {record.imagePath && (
                        <Image
                            source={{ uri: getImageUrl(record.imagePath) }}
                            style={styles.historyImage}
                            resizeMode="contain"
                        />
                    )}
                </View>
            </View>
        );
    };

    if (loading && !refreshing) {
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
                                    <Text style={styles.logoText}>📊</Text>
                                </View>
                            </View>
                            <Text style={styles.title}>识别历史</Text>
                            <Text style={styles.subtitle}>Recognition History</Text>
                        </View>

                        {/* Filter Button */}
                        <View style={styles.filterSection}>
                            <TouchableOpacity
                                onPress={() => setShowFilters(!showFilters)}
                                style={styles.filterButton}
                                activeOpacity={0.8}
                            >
                                <Text style={styles.filterButtonText}>
                                    {showFilters ? '收起筛选' : '筛选'}
                                </Text>
                            </TouchableOpacity>
                        </View>

                        {/* Filters */}
                        {showFilters && (
                            <View style={styles.filtersCard}>
                                <Text style={styles.filtersTitle}>按识别结果筛选：</Text>
                                <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterScroll}>
                                    <TouchableOpacity
                                        style={[styles.filterChip, filterResult === null && styles.filterChipActive]}
                                        onPress={() => {
                                            setFilterResult(null);
                                            setCurrentPage(0);
                                        }}
                                        activeOpacity={0.7}
                                    >
                                        <Text style={[styles.filterChipText, filterResult === null && styles.filterChipTextActive]}>
                                            全部
                                        </Text>
                                    </TouchableOpacity>
                                    {[0, 1, 2, 3, 4, 5, 6, 7, 8, 9].map((num) => (
                                        <TouchableOpacity
                                            key={num}
                                            style={[styles.filterChip, filterResult === num && styles.filterChipActive]}
                                            onPress={() => {
                                                setFilterResult(num);
                                                setCurrentPage(0);
                                            }}
                                            activeOpacity={0.7}
                                        >
                                            <Text style={[styles.filterChipText, filterResult === num && styles.filterChipTextActive]}>
                                                {num}
                                            </Text>
                                        </TouchableOpacity>
                                    ))}
                                </ScrollView>
                            </View>
                        )}

                        {/* Statistics and Actions Card */}
                        <View style={styles.statsCard}>
                            <View style={styles.statsRow}>
                                <Text style={styles.statsText}>共 {totalRecords} 条记录</Text>
                                {selectionMode && (
                                    <Text style={styles.statsText}>已选择 {selectedIds.length} 条</Text>
                                )}
                            </View>

                            {/* Action Bar */}
                            <View style={styles.actionBar}>
                                {!selectionMode ? (
                                    <>
                                        <TouchableOpacity
                                            style={styles.actionButton}
                                            onPress={() => setSelectionMode(true)}
                                            activeOpacity={0.8}
                                        >
                                            <Text style={styles.actionButtonText}>批量管理</Text>
                                        </TouchableOpacity>
                                        <TouchableOpacity
                                            style={styles.actionButton}
                                            onPress={handleRefresh}
                                            activeOpacity={0.8}
                                        >
                                            <Text style={styles.actionButtonText}>刷新</Text>
                                        </TouchableOpacity>
                                    </>
                                ) : (
                                    <>
                                        <TouchableOpacity
                                            style={styles.cancelButton}
                                            onPress={() => {
                                                setSelectionMode(false);
                                                setSelectedIds([]);
                                            }}
                                            activeOpacity={0.8}
                                        >
                                            <Text style={styles.cancelButtonText}>取消</Text>
                                        </TouchableOpacity>
                                        <TouchableOpacity
                                            style={[styles.deleteAllButton, selectedIds.length === 0 && styles.deleteAllButtonDisabled]}
                                            onPress={handleBatchDelete}
                                            disabled={selectedIds.length === 0}
                                            activeOpacity={0.8}
                                        >
                                            <Text style={styles.deleteAllButtonText}>删除选中</Text>
                                        </TouchableOpacity>
                                    </>
                                )}
                            </View>
                        </View>

                        {/* History List */}
                        {history.length === 0 ? (
                            <View style={styles.emptyCard}>
                                <Text style={styles.emptyIcon}>📊</Text>
                                <Text style={styles.emptyText}>暂无识别记录</Text>
                                <Text style={styles.emptySubtext}>开始使用识别功能吧</Text>
                            </View>
                        ) : (
                            <>
                                {history.map(renderHistoryItem)}

                                {/* Pagination */}
                                {totalPages > 1 && (
                                    <View style={styles.pagination}>
                                        <TouchableOpacity
                                            style={[styles.pageButton, currentPage === 0 && styles.pageButtonDisabled]}
                                            onPress={() => setCurrentPage(currentPage - 1)}
                                            disabled={currentPage === 0}
                                            activeOpacity={0.8}
                                        >
                                            <Text style={styles.pageButtonText}>上一页</Text>
                                        </TouchableOpacity>
                                        <Text style={styles.pageInfo}>
                                            {currentPage + 1} / {totalPages}
                                        </Text>
                                        <TouchableOpacity
                                            style={[styles.pageButton, currentPage >= totalPages - 1 && styles.pageButtonDisabled]}
                                            onPress={() => setCurrentPage(currentPage + 1)}
                                            disabled={currentPage >= totalPages - 1}
                                            activeOpacity={0.8}
                                        >
                                            <Text style={styles.pageButtonText}>下一页</Text>
                                        </TouchableOpacity>
                                    </View>
                                )}
                            </>
                        )}
                    </Animated.View>
                </ScrollView>
            </KeyboardAvoidingView>

            {/* Feedback Modal */}
            <Modal
                visible={showFeedbackModal}
                transparent={true}
                animationType="slide"
                onRequestClose={() => setShowFeedbackModal(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <Text style={styles.modalTitle}>提交反馈</Text>
                        <Text style={styles.modalSubtitle}>
                            原识别结果: {selectedRecord?.recognitionResult || "（连续数字）"}
                        </Text>

                        <View style={styles.modalInput}>
                            <Text style={styles.modalLabel}>正确结果 *</Text>
                            <TextInput
                                style={styles.textInput}
                                placeholder="请输入你的预期结果"
                                placeholderTextColor="#94a3b8"
                                value={correctResult}
                                onChangeText={setCorrectResult}
                                keyboardType="number-pad"
                            />
                        </View>

                        <View style={styles.modalInput}>
                            <Text style={styles.modalLabel}>反馈类型</Text>
                            <View style={styles.feedbackTypeContainer}>
                                {[
                                    { value: 'WRONG_RESULT', label: '识别错误' },
                                    { value: 'LOW_CONFIDENCE', label: '置信度低' },
                                    { value: 'OTHER', label: '其他' },
                                ].map((type) => (
                                    <TouchableOpacity
                                        key={type.value}
                                        style={[
                                            styles.typeChip,
                                            feedbackType === type.value && styles.typeChipActive,
                                        ]}
                                        onPress={() => setFeedbackType(type.value)}
                                    >
                                        <Text
                                            style={[
                                                styles.typeChipText,
                                                feedbackType === type.value && styles.typeChipTextActive,
                                            ]}
                                        >
                                            {type.label}
                                        </Text>
                                    </TouchableOpacity>
                                ))}
                            </View>
                        </View>

                        <View style={styles.modalInput}>
                            <Text style={styles.modalLabel}>反馈说明 (可选)</Text>
                            <TextInput
                                style={[styles.textInput, styles.textArea]}
                                placeholder="请描述问题或建议"
                                placeholderTextColor="#94a3b8"
                                value={feedbackReason}
                                onChangeText={setFeedbackReason}
                                multiline
                                numberOfLines={3}
                            />
                        </View>

                        <View style={styles.modalInput}>
                            <Text style={styles.modalLabel}>质量评分 (1-5)</Text>
                            <View style={styles.scoreContainer}>
                                {[1, 2, 3, 4, 5].map((score) => (
                                    <TouchableOpacity
                                        key={score}
                                        style={[
                                            styles.scoreButton,
                                            qualityScore === score && styles.scoreButtonActive,
                                        ]}
                                        onPress={() => setQualityScore(score)}
                                    >
                                        <Text
                                            style={[
                                                styles.scoreButtonText,
                                                qualityScore === score && styles.scoreButtonTextActive,
                                            ]}
                                        >
                                            {score}
                                        </Text>
                                    </TouchableOpacity>
                                ))}
                            </View>
                        </View>

                        <View style={styles.modalActions}>
                            <TouchableOpacity
                                style={styles.modalCancelButton}
                                onPress={() => setShowFeedbackModal(false)}
                            >
                                <Text style={styles.modalCancelButtonText}>取消</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.modalSubmitButton}
                                onPress={handleSubmitFeedback}
                            >
                                <Text style={styles.modalSubmitButtonText}>提交</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>
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
        padding: 20,
        paddingTop: Platform.OS === 'ios' ? 70 : 50,
        paddingBottom: 40,
    },
    contentContainer: {
        width: '100%',
        maxWidth: 440,
        alignSelf: 'center',
    },
    header: {
        alignItems: 'center',
        marginBottom: 20,
    },
    logoContainer: {
        marginBottom: 12,
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
        marginBottom: 4,
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
        marginBottom: 0,
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
    filterSection: {
        marginBottom: 16,
        alignItems: 'flex-end',
    },
    filterButton: {
        paddingVertical: 10,
        paddingHorizontal: 20,
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 20,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.1,
        shadowRadius: 8,
        elevation: 4,
    },
    filterButtonText: {
        color: '#667eea',
        fontSize: 14,
        fontWeight: '700',
    },
    filtersCard: {
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 24,
        padding: 20,
        marginBottom: 20,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.1,
        shadowRadius: 20,
        elevation: 8,
    },
    filtersTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: '#334155',
        marginBottom: 12,
    },
    filterScroll: {
        marginHorizontal: -4,
    },
    filterChip: {
        paddingHorizontal: 18,
        paddingVertical: 10,
        backgroundColor: '#f1f5f9',
        borderRadius: 20,
        marginRight: 10,
        borderWidth: 2,
        borderColor: '#e2e8f0',
    },
    filterChipActive: {
        backgroundColor: '#667eea',
        borderColor: '#667eea',
    },
    filterChipText: {
        fontSize: 14,
        color: '#64748b',
        fontWeight: '600',
    },
    filterChipTextActive: {
        color: '#ffffff',
    },
    statsCard: {
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 24,
        padding: 20,
        marginBottom: 20,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.1,
        shadowRadius: 20,
        elevation: 8,
    },
    statsRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 16,
    },
    statsText: {
        fontSize: 15,
        color: '#334155',
        fontWeight: '600',
    },
    actionBar: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        gap: 12,
    },
    actionButton: {
        flex: 1,
        paddingVertical: 12,
        paddingHorizontal: 20,
        backgroundColor: '#667eea',
        borderRadius: 16,
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#667eea',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 5,
    },
    actionButtonText: {
        color: '#ffffff',
        fontSize: 15,
        fontWeight: '700',
    },
    cancelButton: {
        flex: 1,
        paddingVertical: 12,
        paddingHorizontal: 20,
        backgroundColor: '#64748b',
        borderRadius: 16,
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#64748b',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.2,
        shadowRadius: 8,
        elevation: 4,
    },
    cancelButtonText: {
        color: '#ffffff',
        fontSize: 15,
        fontWeight: '700',
    },
    deleteAllButton: {
        flex: 1,
        paddingVertical: 12,
        paddingHorizontal: 20,
        backgroundColor: '#ef4444',
        borderRadius: 16,
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#ef4444',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 5,
    },
    deleteAllButtonDisabled: {
        backgroundColor: '#fca5a5',
        opacity: 0.6,
    },
    deleteAllButtonText: {
        color: '#ffffff',
        fontSize: 15,
        fontWeight: '700',
    },
    historyItem: {
        flexDirection: 'row',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 24,
        padding: 20,
        marginBottom: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.1,
        shadowRadius: 20,
        elevation: 8,
    },
    historyImage: {
        width: '100%',
        height: 200,
        borderRadius: 16,
        marginTop: 12,
        backgroundColor: '#f1f5f9',
    },
    historyItemSelected: {
        backgroundColor: 'rgba(102, 126, 234, 0.1)',
        borderWidth: 2,
        borderColor: '#667eea',
    },
    checkbox: {
        marginRight: 12,
        justifyContent: 'center',
    },
    checkboxInner: {
        width: 24,
        height: 24,
        borderRadius: 12,
        borderWidth: 2,
        borderColor: '#cbd5e1',
        justifyContent: 'center',
        alignItems: 'center',
    },
    checkboxChecked: {
        backgroundColor: '#667eea',
        borderColor: '#667eea',
    },
    checkmark: {
        color: '#ffffff',
        fontSize: 14,
        fontWeight: 'bold',
    },
    historyContent: {
        flex: 1,
    },
    historyHeader: {
        flexDirection: 'row',
        marginBottom: 12,
    },
    resultBadge: {
        width: 56,
        height: 56,
        borderRadius: 16,
        backgroundColor: '#667eea',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 16,
        shadowColor: '#667eea',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 5,
    },
    resultText: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#ffffff',
    },
    historyInfo: {
        flex: 1,
        justifyContent: 'center',
    },
    historyDate: {
        fontSize: 13,
        color: '#64748b',
        marginBottom: 4,
    },
    confidenceContainer: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    confidenceDot: {
        width: 8,
        height: 8,
        borderRadius: 4,
        marginRight: 6,
    },
    confidenceText: {
        fontSize: 13,
        color: '#64748b',
        fontWeight: '500',
    },
    historyMeta: {
        marginBottom: 12,
    },
    metaItem: {
        flexDirection: 'row',
        marginBottom: 4,
    },
    metaLabel: {
        fontSize: 12,
        color: '#94a3b8',
        fontWeight: '500',
    },
    metaValue: {
        fontSize: 12,
        color: '#334155',
        fontWeight: '600',
    },
    historyActions: {
        flexDirection: 'row',
        justifyContent: 'flex-end',
    },
    feedbackButton: {
        paddingVertical: 10,
        paddingHorizontal: 20,
        backgroundColor: '#f59e0b',
        borderRadius: 16,
        marginRight: 10,
        shadowColor: '#f59e0b',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 4,
    },
    feedbackButtonText: {
        color: '#ffffff',
        fontSize: 14,
        fontWeight: '700',
    },
    deleteButton: {
        paddingVertical: 10,
        paddingHorizontal: 20,
        backgroundColor: '#ef4444',
        borderRadius: 16,
        shadowColor: '#ef4444',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 4,
    },
    deleteButtonText: {
        color: '#ffffff',
        fontSize: 14,
        fontWeight: '700',
    },
    emptyCard: {
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 24,
        padding: 40,
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.1,
        shadowRadius: 20,
        elevation: 8,
    },
    emptyIcon: {
        fontSize: 64,
        marginBottom: 16,
    },
    emptyText: {
        fontSize: 18,
        fontWeight: '600',
        color: '#334155',
        marginBottom: 8,
    },
    emptySubtext: {
        fontSize: 14,
        color: '#94a3b8',
    },
    pagination: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        marginTop: 20,
        marginBottom: 20,
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 24,
        padding: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.1,
        shadowRadius: 20,
        elevation: 8,
    },
    pageButton: {
        paddingVertical: 12,
        paddingHorizontal: 24,
        backgroundColor: '#667eea',
        borderRadius: 16,
        marginHorizontal: 8,
        shadowColor: '#667eea',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 5,
    },
    pageButtonDisabled: {
        backgroundColor: '#cbd5e1',
        shadowOpacity: 0,
        elevation: 0,
    },
    pageButtonText: {
        color: '#ffffff',
        fontSize: 15,
        fontWeight: '700',
    },
    pageInfo: {
        fontSize: 14,
        color: '#64748b',
        fontWeight: '500',
        marginHorizontal: 12,
    },
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        justifyContent: 'center',
        alignItems: 'center',
        padding: 20,
    },
    modalContent: {
        backgroundColor: '#ffffff',
        borderRadius: 24,
        padding: 24,
        width: '100%',
        maxWidth: 400,
    },
    modalTitle: {
        fontSize: 22,
        fontWeight: 'bold',
        color: '#1e293b',
        marginBottom: 8,
    },
    modalSubtitle: {
        fontSize: 14,
        color: '#64748b',
        marginBottom: 20,
    },
    modalInput: {
        marginBottom: 20,
    },
    modalLabel: {
        fontSize: 14,
        fontWeight: '600',
        color: '#334155',
        marginBottom: 8,
    },
    textInput: {
        backgroundColor: '#f8fafc',
        borderRadius: 12,
        borderWidth: 2,
        borderColor: '#e2e8f0',
        paddingHorizontal: 16,
        paddingVertical: 12,
        fontSize: 16,
        color: '#1e293b',
    },
    textArea: {
        height: 80,
        textAlignVertical: 'top',
    },
    feedbackTypeContainer: {
        flexDirection: 'row',
        flexWrap: 'wrap',
    },
    typeChip: {
        paddingHorizontal: 16,
        paddingVertical: 10,
        backgroundColor: '#f1f5f9',
        borderRadius: 12,
        marginRight: 8,
        marginBottom: 8,
        borderWidth: 2,
        borderColor: '#e2e8f0',
    },
    typeChipActive: {
        backgroundColor: '#667eea',
        borderColor: '#667eea',
    },
    typeChipText: {
        fontSize: 14,
        color: '#64748b',
        fontWeight: '500',
    },
    typeChipTextActive: {
        color: '#ffffff',
    },
    scoreContainer: {
        flexDirection: 'row',
        justifyContent: 'space-between',
    },
    scoreButton: {
        flex: 1,
        paddingVertical: 12,
        backgroundColor: '#f1f5f9',
        borderRadius: 12,
        marginHorizontal: 4,
        alignItems: 'center',
        borderWidth: 2,
        borderColor: '#e2e8f0',
    },
    scoreButtonActive: {
        backgroundColor: '#f59e0b',
        borderColor: '#f59e0b',
    },
    scoreButtonText: {
        fontSize: 16,
        color: '#64748b',
        fontWeight: '600',
    },
    scoreButtonTextActive: {
        color: '#ffffff',
    },
    modalActions: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginTop: 20,
    },
    modalCancelButton: {
        flex: 1,
        paddingVertical: 14,
        backgroundColor: '#f1f5f9',
        borderRadius: 12,
        marginRight: 8,
        alignItems: 'center',
    },
    modalCancelButtonText: {
        color: '#64748b',
        fontSize: 16,
        fontWeight: '600',
    },
    modalSubmitButton: {
        flex: 1,
        paddingVertical: 14,
        backgroundColor: '#667eea',
        borderRadius: 12,
        marginLeft: 8,
        alignItems: 'center',
    },
    modalSubmitButtonText: {
        color: '#ffffff',
        fontSize: 16,
        fontWeight: '600',
    },
});

export default HistoryScreen;