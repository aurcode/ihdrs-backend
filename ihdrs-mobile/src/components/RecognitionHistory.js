import React from 'react';
import { View, Text, StyleSheet, FlatList } from 'react-native';

/**
 * RecognitionHistory Component
 * Displays history of digit recognitions with probabilities
 */
const RecognitionHistory = ({ history }) => {
  const renderHistoryItem = ({ item }) => (
    <View style={styles.historyItem}>
      <View style={styles.historyHeader}>
        <Text style={styles.historyDigit}>{item.digit}</Text>
        <Text style={styles.historyConfidence}>
          {(item.confidence * 100).toFixed(1)}%
        </Text>
        <Text style={styles.historyTime}>{item.timestamp}</Text>
      </View>
      
      {/* Show all probabilities */}
      {item.probabilities && (
        <View style={styles.probabilitiesContainer}>
          {item.probabilities.map((prob, index) => (
            <View key={index} style={styles.probabilityRow}>
              <Text style={styles.probabilityDigit}>{index}</Text>
              <View style={styles.probabilityBarContainer}>
                <View 
                  style={[
                    styles.probabilityBar, 
                    { width: `${prob * 100}%` }
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
    shadowOffset: { width: 0, height: 2 },
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
