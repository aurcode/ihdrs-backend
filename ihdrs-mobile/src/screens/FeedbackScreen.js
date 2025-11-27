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
    Platform,
    KeyboardAvoidingView,
} from 'react-native';
import feedbackService from '../services/feedbackService';
import { Image } from 'react-native';

const { width } = Dimensions.get('window');

const FeedbackScreen = ({ user, token, onCancel }) => {
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [feedbackList, setFeedbackList] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [totalRecords, setTotalRecords] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    // Filter states
    const [filterStatus, setFilterStatus] = useState(null);
    const [filterType, setFilterType] = useState(null);
    const [showFilters, setShowFilters] = useState(false);

    // Detail modal states
    const [showDetailModal, setShowDetailModal] = useState(false);
    const [selectedFeedback, setSelectedFeedback] = useState(null);

    // Animation
    const [fadeAnim] = useState(new Animated.Value(0));
    const [slideAnim] = useState(new Animated.Value(50));

    useEffect(() => {
        loadFeedbackList();

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
    }, [currentPage, filterStatus, filterType]);

    const loadFeedbackList = async () => {
        try {
            setLoading(true);
            const response = await feedbackService.getUserFeedbackList(
                token,
                currentPage,
                pageSize,
                filterStatus,
                filterType
            );

            if (response.success) {
                const data = response.data;
                setFeedbackList(data.records || []);
                setTotalRecords(data.total || 0);
                setTotalPages(data.pages || 0);
            } else {
                Alert.alert('错误', response.error || '加载反馈列表失败');
            }
        } catch (error) {
            Alert.alert('错误', '加载反馈列表失败');
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    const handleRefresh = () => {
        setRefreshing(true);
        setCurrentPage(1);
        loadFeedbackList();
    };

    const handleViewDetail = async (feedback) => {
        setSelectedFeedback(feedback);
        setShowDetailModal(true);
    };

    const BASE_URL = "http://10.0.2.2:8080";

    const getImageUrl = (path) => {
        if (!path) return null;
        return BASE_URL + path;
    };

    const handleDeleteFeedback = (feedbackId) => {
        Alert.alert(
            '确认删除',
            '确定要删除这条反馈吗？只能删除待审核的反馈。',
            [
                { text: '取消', style: 'cancel' },
                {
                    text: '删除',
                    style: 'destructive',
                    onPress: async () => {
                        const response = await feedbackService.deleteFeedback(token, feedbackId);
                        if (response.success) {
                            Alert.alert('成功', '反馈已删除');
                            loadFeedbackList();
                        } else {
                            Alert.alert('删除失败', response.error);
                        }
                    },
                },
            ]
        );
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

    const getStatusColor = (status) => {
        switch (status) {
            case 'PENDING':
                return '#f59e0b';
            case 'ACCEPTED':
                return '#10b981';
            case 'REJECTED':
                return '#ef4444';
            default:
                return '#94a3b8';
        }
    };

    const getStatusText = (status) => {
        switch (status) {
            case 'PENDING':
                return '待审核';
            case 'ACCEPTED':
                return '已采纳';
            case 'REJECTED':
                return '已拒绝';
            default:
                return '未知';
        }
    };

    const getTypeText = (type) => {
        switch (type) {
            case 'WRONG_RESULT':
                return '识别错误';
            case 'LOW_CONFIDENCE':
                return '置信度低';
            case 'OTHER':
                return '其他';
            default:
                return '未知';
        }
    };

    const renderFeedbackItem = (feedback) => {
        const statusColor = getStatusColor(feedback.status);

        return (
            <TouchableOpacity
                key={feedback.feedbackId}
                style={styles.feedbackItem}
                onPress={() => handleViewDetail(feedback)}
                activeOpacity={0.7}
            >
                <View style={styles.feedbackHeader}>
                    <View style={styles.feedbackResults}>
                        <View style={styles.resultBox}>
                            <Text style={styles.resultLabel}>原结果</Text>
                            <Text style={styles.resultValue}>{feedback.originalResult}</Text>
                        </View>
                        <Text style={styles.arrow}>→</Text>
                        <View style={styles.resultBox}>
                            <Text style={styles.resultLabel}>正确结果</Text>
                            <Text style={[styles.resultValue, { color: '#10b981' }]}>
                                {feedback.correctResult}
                            </Text>
                        </View>
                    </View>
                    <View style={[styles.statusBadge, { backgroundColor: statusColor }]}>
                        <Text style={styles.statusText}>{getStatusText(feedback.status)}</Text>
                    </View>
                </View>

                <View style={styles.feedbackMeta}>
                    <View style={styles.metaRow}>
                        <Text style={styles.metaLabel}>反馈类型：</Text>
                        <Text style={styles.metaValue}>{getTypeText(feedback.feedbackType)}</Text>
                    </View>
                    <View style={styles.metaRow}>
                        <Text style={styles.metaLabel}>提交时间：</Text>
                        <Text style={styles.metaValue}>{formatDate(feedback.createTime)}</Text>
                    </View>
                    {feedback.qualityScore && (
                        <View style={styles.metaRow}>
                            <Text style={styles.metaLabel}>质量评分：</Text>
                            <View style={styles.scoreStars}>
                                {[1, 2, 3, 4, 5].map((star) => (
                                    <Text
                                        key={star}
                                        style={[
                                            styles.star,
                                            star <= feedback.qualityScore && styles.starActive,
                                        ]}
                                    >
                                        ★
                                    </Text>
                                ))}
                            </View>
                        </View>
                    )}
                    {feedback.recordInfo && (
                        <View style={styles.metaRow}>
                            <Text style={styles.metaLabel}>使用模型：</Text>
                            <Text style={styles.metaValue}>
                                {feedback.modelName || 'N/A'} v{feedback.modelVersion || 'N/A'}
                            </Text>
                        </View>
                    )}
                </View>

                {feedback.feedbackReason && (
                    <View style={styles.reasonContainer}>
                        <Text style={styles.reasonLabel}>反馈说明：</Text>
                        <Text style={styles.reasonText} numberOfLines={2}>
                            {feedback.feedbackReason}
                        </Text>
                    </View>
                )}

                {feedback.status !== 'PENDING' && feedback.reviewTime && (
                    <View style={styles.reviewContainer}>
                        <View style={styles.reviewHeader}>
                            <Text style={styles.reviewLabel}>审核信息</Text>
                            <Text style={styles.reviewDate}>{formatDate(feedback.reviewTime)}</Text>
                        </View>
                        {feedback.reviewerName && (
                            <Text style={styles.reviewerName}>审核人：{feedback.reviewerName}</Text>
                        )}
                        {feedback.reviewNote && (
                            <Text style={styles.reviewNote}>{feedback.reviewNote}</Text>
                        )}
                    </View>
                )}

                <View style={styles.feedbackActions}>
                    <TouchableOpacity
                        style={styles.viewButton}
                        onPress={() => handleViewDetail(feedback)}
                    >
                        <Text style={styles.viewButtonText}>查看详情</Text>
                    </TouchableOpacity>
                    {feedback.status === 'PENDING' && (
                        <TouchableOpacity
                            style={styles.deleteButton}
                            onPress={() => handleDeleteFeedback(feedback.feedbackId)}
                        >
                            <Text style={styles.deleteButtonText}>删除</Text>
                        </TouchableOpacity>
                    )}
                </View>
                {feedback.recordInfo?.imagePath && (
                    <View style={{ marginTop: 12, alignItems: 'center' }}>
                        <Image
                            source={{ uri: getImageUrl(feedback.recordInfo?.imagePath) }}
                            style={{ width: 120, height: 120, borderRadius: 12 }}
                            resizeMode="contain"
                        />
                    </View>
                )}
            </TouchableOpacity>
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
                                    <Text style={styles.logoText}>💬</Text>
                                </View>
                            </View>
                            <Text style={styles.title}>我的反馈</Text>
                            <Text style={styles.subtitle}>My Feedback</Text>
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
                                <View style={styles.filterSectionInner}>
                                    <Text style={styles.filterSectionTitle}>审核状态：</Text>
                                    <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterScroll}>
                                        <TouchableOpacity
                                            style={[styles.filterChip, filterStatus === null && styles.filterChipActive]}
                                            onPress={() => {
                                                setFilterStatus(null);
                                                setCurrentPage(1);
                                            }}
                                            activeOpacity={0.7}
                                        >
                                            <Text style={[styles.filterChipText, filterStatus === null && styles.filterChipTextActive]}>
                                                全部
                                            </Text>
                                        </TouchableOpacity>
                                        {['PENDING', 'ACCEPTED', 'REJECTED'].map((status) => (
                                            <TouchableOpacity
                                                key={status}
                                                style={[styles.filterChip, filterStatus === status && styles.filterChipActive]}
                                                onPress={() => {
                                                    setFilterStatus(status);
                                                    setCurrentPage(1);
                                                }}
                                                activeOpacity={0.7}
                                            >
                                                <Text style={[styles.filterChipText, filterStatus === status && styles.filterChipTextActive]}>
                                                    {getStatusText(status)}
                                                </Text>
                                            </TouchableOpacity>
                                        ))}
                                    </ScrollView>
                                </View>

                                <View style={styles.filterSectionInner}>
                                    <Text style={styles.filterSectionTitle}>反馈类型：</Text>
                                    <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterScroll}>
                                        <TouchableOpacity
                                            style={[styles.filterChip, filterType === null && styles.filterChipActive]}
                                            onPress={() => {
                                                setFilterType(null);
                                                setCurrentPage(1);
                                            }}
                                            activeOpacity={0.7}
                                        >
                                            <Text style={[styles.filterChipText, filterType === null && styles.filterChipTextActive]}>
                                                全部
                                            </Text>
                                        </TouchableOpacity>
                                        {['WRONG_RESULT', 'LOW_CONFIDENCE', 'OTHER'].map((type) => (
                                            <TouchableOpacity
                                                key={type}
                                                style={[styles.filterChip, filterType === type && styles.filterChipActive]}
                                                onPress={() => {
                                                    setFilterType(type);
                                                    setCurrentPage(1);
                                                }}
                                                activeOpacity={0.7}
                                            >
                                                <Text style={[styles.filterChipText, filterType === type && styles.filterChipTextActive]}>
                                                    {getTypeText(type)}
                                                </Text>
                                            </TouchableOpacity>
                                        ))}
                                    </ScrollView>
                                </View>
                            </View>
                        )}

                        {/* Statistics Card */}
                        <View style={styles.statsCard}>
                            <Text style={styles.statsText}>共 {totalRecords} 条反馈</Text>
                            <TouchableOpacity onPress={handleRefresh} style={styles.refreshButton} activeOpacity={0.8}>
                                <Text style={styles.refreshButtonText}>刷新</Text>
                            </TouchableOpacity>
                        </View>

                        {/* Feedback List */}
                        {feedbackList.length === 0 ? (
                            <View style={styles.emptyCard}>
                                <Text style={styles.emptyIcon}>💬</Text>
                                <Text style={styles.emptyText}>暂无反馈记录</Text>
                                <Text style={styles.emptySubtext}>提交识别反馈帮助我们改进</Text>
                            </View>
                        ) : (
                            <>
                                {feedbackList.map(renderFeedbackItem)}

                                {/* Pagination */}
                                {totalPages > 1 && (
                                    <View style={styles.pagination}>
                                        <TouchableOpacity
                                            style={[styles.pageButton, currentPage === 1 && styles.pageButtonDisabled]}
                                            onPress={() => setCurrentPage(currentPage - 1)}
                                            disabled={currentPage === 1}
                                            activeOpacity={0.8}
                                        >
                                            <Text style={styles.pageButtonText}>上一页</Text>
                                        </TouchableOpacity>
                                        <Text style={styles.pageInfo}>
                                            {currentPage} / {totalPages}
                                        </Text>
                                        <TouchableOpacity
                                            style={[styles.pageButton, currentPage >= totalPages && styles.pageButtonDisabled]}
                                            onPress={() => setCurrentPage(currentPage + 1)}
                                            disabled={currentPage >= totalPages}
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

            {/* Detail Modal */}
            <Modal
                visible={showDetailModal}
                transparent={true}
                animationType="slide"
                onRequestClose={() => setShowDetailModal(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>反馈详情</Text>
                            <TouchableOpacity
                                onPress={() => setShowDetailModal(false)}
                                style={styles.closeButton}
                            >
                                <Text style={styles.closeButtonText}>✕</Text>
                            </TouchableOpacity>
                        </View>

                        {selectedFeedback && (
                            <ScrollView showsVerticalScrollIndicator={false}>
                                <View style={styles.detailSection}>
                                    <Text style={styles.detailLabel}>识别结果</Text>
                                    <View style={styles.detailResults}>
                                        <View style={styles.detailResultBox}>
                                            <Text style={styles.detailResultLabel}>原结果</Text>
                                            <Text style={styles.detailResultValue}>
                                                {selectedFeedback.originalResult}
                                            </Text>
                                        </View>
                                        <Text style={styles.detailArrow}>→</Text>
                                        <View style={styles.detailResultBox}>
                                            <Text style={styles.detailResultLabel}>正确结果</Text>
                                            <Text style={[styles.detailResultValue, { color: '#10b981' }]}>
                                                {selectedFeedback.correctResult}
                                            </Text>
                                        </View>
                                    </View>
                                </View>

                                <View style={styles.detailSection}>
                                    <Text style={styles.detailLabel}>反馈信息</Text>
                                    <View style={styles.detailInfo}>
                                        <View style={styles.detailRow}>
                                            <Text style={styles.detailRowLabel}>反馈类型：</Text>
                                            <Text style={styles.detailRowValue}>
                                                {getTypeText(selectedFeedback.feedbackType)}
                                            </Text>
                                        </View>
                                        <View style={styles.detailRow}>
                                            <Text style={styles.detailRowLabel}>提交时间：</Text>
                                            <Text style={styles.detailRowValue}>
                                                {formatDate(selectedFeedback.createTime)}
                                            </Text>
                                        </View>
                                        <View style={styles.detailRow}>
                                            <Text style={styles.detailRowLabel}>审核状态：</Text>
                                            <View
                                                style={[
                                                    styles.detailStatusBadge,
                                                    { backgroundColor: getStatusColor(selectedFeedback.status) },
                                                ]}
                                            >
                                                <Text style={styles.detailStatusText}>
                                                    {getStatusText(selectedFeedback.status)}
                                                </Text>
                                            </View>
                                        </View>
                                        {selectedFeedback.qualityScore && (
                                            <View style={styles.detailRow}>
                                                <Text style={styles.detailRowLabel}>质量评分：</Text>
                                                <View style={styles.detailScoreStars}>
                                                    {[1, 2, 3, 4, 5].map((star) => (
                                                        <Text
                                                            key={star}
                                                            style={[
                                                                styles.detailStar,
                                                                star <= selectedFeedback.qualityScore && styles.detailStarActive,
                                                            ]}
                                                        >
                                                            ★
                                                        </Text>
                                                    ))}
                                                </View>
                                            </View>
                                        )}
                                    </View>
                                </View>

                                {selectedFeedback.feedbackReason && (
                                    <View style={styles.detailSection}>
                                        <Text style={styles.detailLabel}>反馈说明</Text>
                                        <Text style={styles.detailReason}>{selectedFeedback.feedbackReason}</Text>
                                        {selectedFeedback.recordInfo?.imagePath && (
                                            <View style={styles.detailSection}>
                                                <Text style={styles.detailLabel}>识别图像</Text>

                                                <Image
                                                    source={{ uri: getImageUrl(selectedFeedback.recordInfo.imagePath) }}
                                                    style={{
                                                        width: '100%',
                                                        height: 300,
                                                        borderRadius: 16,
                                                        backgroundColor: '#f1f5f9',
                                                    }}
                                                    resizeMode="contain"
                                                />
                                            </View>
                                        )}

                                    </View>
                                )}

                                {selectedFeedback.recordInfo && (
                                    <View style={styles.detailSection}>
                                        <Text style={styles.detailLabel}>识别记录信息</Text>
                                        <View style={styles.detailInfo}>
                                            <View style={styles.detailRow}>
                                                <Text style={styles.detailRowLabel}>模型名称：</Text>
                                                <Text style={styles.detailRowValue}>
                                                    {selectedFeedback.modelName || 'N/A'}
                                                </Text>
                                            </View>
                                            <View style={styles.detailRow}>
                                                <Text style={styles.detailRowLabel}>模型版本：</Text>
                                                <Text style={styles.detailRowValue}>
                                                    {selectedFeedback.modelVersion || 'N/A'}
                                                </Text>
                                            </View>
                                            {selectedFeedback.recordInfo.confidence && (
                                                <View style={styles.detailRow}>
                                                    <Text style={styles.detailRowLabel}>置信度：</Text>
                                                    <Text style={styles.detailRowValue}>
                                                        {(parseFloat(selectedFeedback.recordInfo.confidence) * 100).toFixed(1)}%
                                                    </Text>
                                                </View>
                                            )}
                                            {selectedFeedback.recordInfo.recognitionTime && (
                                                <View style={styles.detailRow}>
                                                    <Text style={styles.detailRowLabel}>识别时间：</Text>
                                                    <Text style={styles.detailRowValue}>
                                                        {formatDate(selectedFeedback.recordInfo.recognitionTime)}
                                                    </Text>
                                                </View>
                                            )}
                                        </View>
                                    </View>
                                )}

                                {selectedFeedback.status !== 'PENDING' && selectedFeedback.reviewTime && (
                                    <View style={styles.detailSection}>
                                        <Text style={styles.detailLabel}>审核信息</Text>
                                        <View style={styles.detailInfo}>
                                            <View style={styles.detailRow}>
                                                <Text style={styles.detailRowLabel}>审核时间：</Text>
                                                <Text style={styles.detailRowValue}>
                                                    {formatDate(selectedFeedback.reviewTime)}
                                                </Text>
                                            </View>
                                            {selectedFeedback.reviewerName && (
                                                <View style={styles.detailRow}>
                                                    <Text style={styles.detailRowLabel}>审核人：</Text>
                                                    <Text style={styles.detailRowValue}>
                                                        {selectedFeedback.reviewerName}
                                                    </Text>
                                                </View>
                                            )}
                                            {selectedFeedback.reviewNote && (
                                                <View style={styles.detailRow}>
                                                    <Text style={styles.detailRowLabel}>审核说明：</Text>
                                                    <Text style={styles.detailRowValue}>
                                                        {selectedFeedback.reviewNote}
                                                    </Text>
                                                </View>
                                            )}
                                        </View>
                                    </View>
                                )}
                            </ScrollView>
                        )}
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
    filterSectionInner: {
        marginBottom: 16,
    },
    filterSectionTitle: {
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
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
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
    statsText: {
        fontSize: 15,
        color: '#334155',
        fontWeight: '600',
    },
    refreshButton: {
        paddingVertical: 10,
        paddingHorizontal: 20,
        backgroundColor: '#667eea',
        borderRadius: 16,
        shadowColor: '#667eea',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 5,
    },
    refreshButtonText: {
        color: '#ffffff',
        fontSize: 14,
        fontWeight: '700',
    },
    feedbackItem: {
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
    feedbackHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 12,
    },
    feedbackResults: {
        flexDirection: 'row',
        alignItems: 'center',
        flex: 1,
    },
    resultBox: {
        alignItems: 'center',
    },
    resultLabel: {
        fontSize: 11,
        color: '#94a3b8',
        marginBottom: 4,
    },
    resultValue: {
        fontSize: 28,
        fontWeight: 'bold',
        color: '#334155',
    },
    arrow: {
        fontSize: 20,
        color: '#94a3b8',
        marginHorizontal: 12,
    },
    statusBadge: {
        paddingHorizontal: 14,
        paddingVertical: 8,
        borderRadius: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 3,
    },
    statusText: {
        fontSize: 13,
        color: '#ffffff',
        fontWeight: '700',
    },
    feedbackMeta: {
        marginBottom: 12,
    },
    metaRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 6,
    },
    metaLabel: {
        fontSize: 13,
        color: '#64748b',
        marginRight: 8,
    },
    metaValue: {
        fontSize: 13,
        color: '#334155',
        fontWeight: '500',
    },
    scoreStars: {
        flexDirection: 'row',
    },
    star: {
        fontSize: 14,
        color: '#cbd5e1',
        marginRight: 2,
    },
    starActive: {
        color: '#f59e0b',
    },
    reasonContainer: {
        backgroundColor: '#f8fafc',
        borderRadius: 16,
        padding: 16,
        marginBottom: 12,
        borderWidth: 1,
        borderColor: '#e2e8f0',
    },
    reasonLabel: {
        fontSize: 13,
        color: '#64748b',
        fontWeight: '600',
        marginBottom: 6,
    },
    reasonText: {
        fontSize: 14,
        color: '#334155',
        lineHeight: 20,
    },
    reviewContainer: {
        backgroundColor: '#eff6ff',
        borderRadius: 16,
        padding: 16,
        marginBottom: 12,
        borderLeftWidth: 4,
        borderLeftColor: '#3b82f6',
        borderWidth: 1,
        borderColor: '#dbeafe',
    },
    reviewHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: 8,
    },
    reviewLabel: {
        fontSize: 13,
        color: '#1e40af',
        fontWeight: '600',
    },
    reviewDate: {
        fontSize: 11,
        color: '#64748b',
    },
    reviewerName: {
        fontSize: 12,
        color: '#334155',
        marginBottom: 4,
    },
    reviewNote: {
        fontSize: 13,
        color: '#1e3a8a',
        lineHeight: 18,
    },
    feedbackActions: {
        flexDirection: 'row',
        justifyContent: 'flex-end',
    },
    viewButton: {
        paddingVertical: 10,
        paddingHorizontal: 20,
        backgroundColor: '#667eea',
        borderRadius: 16,
        marginRight: 10,
        shadowColor: '#667eea',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 5,
    },
    viewButtonText: {
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
        elevation: 5,
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
        paddingVertical: 10,
        paddingHorizontal: 20,
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
        fontSize: 14,
        fontWeight: '700',
    },
    pageInfo: {
        fontSize: 15,
        color: '#334155',
        fontWeight: '600',
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
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: 24,
        padding: 24,
        width: '100%',
        maxWidth: 400,
        maxHeight: '80%',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.2,
        shadowRadius: 20,
        elevation: 10,
    },
    modalHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 20,
    },
    modalTitle: {
        fontSize: 22,
        fontWeight: 'bold',
        color: '#1e293b',
    },
    closeButton: {
        width: 36,
        height: 36,
        borderRadius: 18,
        backgroundColor: '#f1f5f9',
        justifyContent: 'center',
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 3,
    },
    closeButtonText: {
        fontSize: 20,
        color: '#64748b',
        fontWeight: '600',
    },
    detailSection: {
        marginBottom: 20,
    },
    detailLabel: {
        fontSize: 16,
        fontWeight: '600',
        color: '#334155',
        marginBottom: 12,
    },
    detailResults: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 20,
    },
    detailResultBox: {
        alignItems: 'center',
    },
    detailResultLabel: {
        fontSize: 12,
        color: '#94a3b8',
        marginBottom: 8,
    },
    detailResultValue: {
        fontSize: 36,
        fontWeight: 'bold',
        color: '#334155',
    },
    detailArrow: {
        fontSize: 24,
        color: '#94a3b8',
        marginHorizontal: 20,
    },
    detailInfo: {
        backgroundColor: '#f8fafc',
        borderRadius: 16,
        padding: 16,
        borderWidth: 1,
        borderColor: '#e2e8f0',
    },
    detailRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 12,
    },
    detailRowLabel: {
        fontSize: 14,
        color: '#64748b',
        marginRight: 8,
        minWidth: 80,
        fontWeight: '600',
    },
    detailRowValue: {
        fontSize: 14,
        color: '#334155',
        fontWeight: '500',
        flex: 1,
    },
    detailStatusBadge: {
        paddingHorizontal: 14,
        paddingVertical: 8,
        borderRadius: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 3,
    },
    detailStatusText: {
        fontSize: 13,
        color: '#ffffff',
        fontWeight: '700',
    },
    detailScoreStars: {
        flexDirection: 'row',
    },
    detailStar: {
        fontSize: 16,
        color: '#cbd5e1',
        marginRight: 2,
    },
    detailStarActive: {
        color: '#f59e0b',
    },
    detailReason: {
        backgroundColor: '#f8fafc',
        borderRadius: 16,
        padding: 16,
        fontSize: 14,
        color: '#334155',
        lineHeight: 20,
        borderWidth: 1,
        borderColor: '#e2e8f0',
    },
});

export default FeedbackScreen;