import React from 'react';
import {View, Text, StyleSheet, FlatList} from 'react-native';

/**
 * RecognitionHistory Component
 * Displays history of digit recognitions with probabilities
 */
const RecognitionHistory = ({history}) => {
    const renderHistoryItem = ({item}) => {
        // 判断是否为多数字识别
        const isMultiDigit = item.type === "MULTI";

        return (
            <View style={styles.historyItem}>
                <View style={styles.historyHeader}>
                    {isMultiDigit ? (
                        <>
                            <Text style={styles.historySequence}>{item.sequence}</Text>
                        </>
                    ) : (
                        <Text style={styles.historyDigit}>{item.digit}</Text>
                    )}

                    <Text style={styles.historyConfidence}>
                        {isMultiDigit && item.details
                            ? `${(item.details.reduce((sum, d) => sum + d.confidence, 0) / item.details.length * 100).toFixed(1)}%`
                            : `${(item.confidence * 100).toFixed(1)}%`
                        }
                    </Text>
                    <Text style={styles.historyTime}>{item.timestamp}</Text>
                </View>

                {/* 多数字详情 */}
                {isMultiDigit && item.details && (
                    <View style={styles.multiDetailsContainer}>
                        {item.details.map((detail, index) => (
                            <View key={index} style={styles.digitDetail}>
                                <Text style={styles.digitLabel}>数字 {index + 1}</Text>
                                <Text style={styles.digitValue}>{detail.digit}</Text>
                                <Text style={styles.digitConfidence}>
                                    {(detail.confidence * 100).toFixed(1)}%
                                </Text>
                            </View>
                        ))}
                    </View>
                )}

                {/* 单数字概率分布 */}
                {!isMultiDigit && item.probabilities && (
                    <View style={styles.probabilitiesContainer}>
                        {item.probabilities.map((prob, index) => (
                            <View key={index} style={styles.probabilityRow}>
                                <Text style={styles.probabilityDigit}>{index}</Text>
                                <View style={styles.probabilityBarContainer}>
                                    <View
                                        style={[
                                            styles.probabilityBar,
                                            {width: `${prob * 100}%`}
                                        ]}
                                    />
                                </View>
                                <Text style={styles.probabilityValue}>
                                    {(prob * 100).toFixed(1)}%
                                </Text>
                            </View>
                        ))}
                    </View>
                )}
            </View>
        );
    };

    return (
        <View style={styles.container}>
            <Text style={styles.title}>Recognition History</Text>
            <FlatList
                data={history}
                renderItem={renderHistoryItem}
                keyExtractor={(item) => item.id.toString()}
                scrollEnabled={false}
                ListEmptyComponent={
                    <Text style={styles.emptyText}>No recognition history yet</Text>
                }
            />
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
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
    title: {
        fontSize: 20,
        fontWeight: 'bold',
        color: '#1f2937',
        marginBottom: 15,
    },
    historyItem: {
        backgroundColor: '#f9fafb',
        borderRadius: 8,
        padding: 15,
        marginBottom: 12,
        borderLeftWidth: 4,
        borderLeftColor: '#6366f1',
    },
    historyHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 12,
    },
    historyDigit: {
        fontSize: 32,
        fontWeight: 'bold',
        color: '#1f2937',
        minWidth: 50,
    },
    historySequence: {
        fontSize: 28,
        fontWeight: 'bold',
        color: '#1f2937',
        minWidth: 100,
        letterSpacing: 2,
    },
    historyLabel: {
        fontSize: 12,
        color: '#6b7280',
        marginLeft: 8,
    },
    historyConfidence: {
        fontSize: 18,
        fontWeight: '600',
        color: '#10b981',
        flex: 1,
        textAlign: 'center',
    },
    historyTime: {
        fontSize: 12,
        color: '#6b7280',
    },
    multiDetailsContainer: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        marginTop: 12,
        gap: 8,
    },
    digitDetail: {
        backgroundColor: '#fff',
        borderRadius: 8,
        padding: 10,
        minWidth: 60,
        alignItems: 'center',
        borderWidth: 1,
        borderColor: '#e5e7eb',
    },
    digitLabel: {
        fontSize: 10,
        color: '#6b7280',
        marginBottom: 4,
    },
    digitValue: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#6366f1',
    },
    digitConfidence: {
        fontSize: 10,
        color: '#10b981',
        marginTop: 2,
    },
    probabilitiesContainer: {
        marginTop: 8,
    },
    probabilityRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginVertical: 4,
    },
    probabilityDigit: {
        fontSize: 14,
        fontWeight: '600',
        color: '#374151',
        width: 20,
    },
    probabilityBarContainer: {
        flex: 1,
        height: 20,
        backgroundColor: '#e5e7eb',
        borderRadius: 4,
        marginHorizontal: 10,
        overflow: 'hidden',
    },
    probabilityBar: {
        height: '100%',
        backgroundColor: '#6366f1',
        borderRadius: 4,
    },
    probabilityValue: {
        fontSize: 12,
        color: '#6b7280',
        width: 50,
        textAlign: 'right',
    },
    emptyText: {
        textAlign: 'center',
        color: '#9ca3af',
        fontSize: 14,
        paddingVertical: 20,
    },
});

export default RecognitionHistory;
