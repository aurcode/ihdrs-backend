// views/models/Training.vue

<template>
  <div class="training-management">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card primary">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32">
                <DataAnalysis/>
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalTasks || 0 }}</div>
              <div class="stat-label">总任务数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card success">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32">
                <CircleCheck/>
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.completedTasks || 0 }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card warning">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32">
                <Loading/>
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.runningTasks || 0 }}</div>
              <div class="stat-label">训练中</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card info">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32">
                <TrendCharts/>
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ (statistics.avgAccuracy * 100).toFixed(2) }}%</div>
              <div class="stat-label">平均准确率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 过滤器 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="任务名称">
          <el-input
              v-model="filterForm.keyword"
              placeholder="请输入任务名称"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon>
                <Search/>
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option label="等待中" value="PENDING"/>
            <el-option label="训练中" value="RUNNING"/>
            <el-option label="已完成" value="COMPLETED"/>
            <el-option label="已失败" value="FAILED"/>
            <el-option label="已取消" value="CANCELLED"/>
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
          <el-button @click="resetFilter" :icon="Refresh">重置</el-button>
          <el-button type="success" @click="showCreateDialog" :icon="Plus">新建训练任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 任务列表 -->
    <el-card class="table-card">
      <el-table
          v-loading="loading"
          :data="taskList"
          stripe
          style="width: 100%"
          @row-click="handleRowClick"
          :row-class-name="tableRowClassName"
      >
        <el-table-column prop="taskName" label="任务名称" min-width="150"/>

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="训练进度" width="200">
          <template #default="{ row }">
            <div v-if="row.status === 'RUNNING' || row.status === 'COMPLETED'">
              <el-progress
                  :percentage="parseFloat(row.progress || 0)"
                  :status="row.status === 'COMPLETED' ? 'success' : undefined"
              />
              <div class="progress-text">
                Epoch: {{ row.currentEpoch || 0 }}/{{ row.totalEpochs }}
              </div>
            </div>
            <div v-else>-</div>
          </template>
        </el-table-column>

        <el-table-column prop="bestAccuracy" label="训练集最佳准确率" width="150">
          <template #default="{ row }">
            {{ row.bestAccuracy ? (row.bestAccuracy * 100).toFixed(2) + '%' : '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="finalAccuracy" label="最终准确率" width="120">
          <template #default="{ row }">
            {{ row.finalAccuracy ? (row.finalAccuracy * 100).toFixed(2) + '%' : '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click.stop="viewDetail(row)">
              <el-icon>
                <View/>
              </el-icon>
              详情
            </el-button>

            <el-button
                v-if="row.status === 'RUNNING'"
                size="small"
                type="warning"
                link
                @click.stop="handleCancelTask(row)"
            >
              <el-icon>
                <VideoPause/>
              </el-icon>
              取消
            </el-button>

            <el-button
                v-if="row.status === 'RUNNING' || row.status === 'COMPLETED'"
                size="small"
                type="success"
                link
                @click.stop="openLogsDialog(row)"
            >
              <el-icon>
                <Document/>
              </el-icon>
              日志弹窗
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 选中任务的训练视图（直接展示在页面中） -->
    <el-card v-if="selectedTask" class="selected-task-card" shadow="never">
      <div class="selected-task-header">
        <div>
          <h3>当前选中任务：{{ selectedTask.taskName }}</h3>
          <div class="selected-task-meta">
            <span>状态：<el-tag :type="getStatusType(selectedTask.status)">{{
                getStatusText(selectedTask.status)
              }}</el-tag></span>
            <span>进度：{{
                (selectedTask.progress || 0).toFixed ? selectedTask.progress.toFixed(2) : selectedTask.progress || 0
              }}%</span>
            <span>Epoch：{{ selectedTask.currentEpoch || 0 }}/{{ selectedTask.totalEpochs }}</span>
            <span>最佳准确率：{{
                selectedTask.bestAccuracy ? (selectedTask.bestAccuracy * 100).toFixed(2) + '%' : '-'
              }}</span>
            <span>最终准确率：{{
                selectedTask.finalAccuracy ? (selectedTask.finalAccuracy * 100).toFixed(2) + '%' : '-'
              }}</span>
            <span>最终损失：{{ selectedTask.finalLoss ? selectedTask.finalLoss.toFixed(6) : '-' }}</span>
          </div>
        </div>
        <div class="selected-task-time">
          <div>开始时间：{{ formatDate(selectedTask.startTime) }}</div>
          <div>结束时间：{{ formatDate(selectedTask.endTime) }}</div>
        </div>
      </div>

      <div class="charts-container" v-loading="inlineLogsLoading">
        <div v-if="!logsDialog.logs.length" class="no-logs-tip">
          暂无训练日志，请等待训练过程中产生日志。
        </div>
        <template v-else>
          <!-- 上方增加一些汇总信息 -->
          <div class="logs-summary">
            <span>最新 Epoch：{{ latestLog?.epoch ?? '-' }}</span>
            <span>最新 Step：{{ latestLog?.step ?? '-' }}</span>
            <span>当前学习率：{{ latestLog?.learningRate ?? '-' }}</span>
            <span>Batch Size：{{ latestLog?.batchSize ?? '-' }}</span>
            <span>最近日志时间：{{ latestLog ? formatDate(latestLog.timestamp) : '-' }}</span>
          </div>

          <el-row :gutter="20">
            <el-col :span="12">
              <div class="chart-item">
                <h4>准确率曲线</h4>
                <v-chart :option="accuracyChartOption" autoresize style="height: 300px"/>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="chart-item">
                <h4>损失曲线</h4>
                <v-chart :option="lossChartOption" autoresize style="height: 300px"/>
              </div>
            </el-col>
          </el-row>
          <el-row :gutter="20" style="margin-top: 10px" v-if="logsDialog.logs.length">
            <el-col :span="24">
              <div class="chart-item">
                <h4>学习率曲线</h4>
                <v-chart :option="lrChartOption" autoresize style="height: 260px"/>
              </div>
            </el-col>
            <el-col :span="24">
              <div class="chart-item">
                <h4>准确率差（过拟合观察）</h4>
                <v-chart :option="gapAccChartOption" autoresize style="height: 260px"/>
              </div>
            </el-col>
            <el-col :span="24">
              <div class="chart-item">
                <h4>每个 Epoch 时长</h4>
                <v-chart :option="epochDurationChartOption" autoresize style="height: 260px"/>
              </div>
            </el-col>
            <el-col :span="24">
              <!-- 混淆矩阵区域 -->
              <div v-if="confusionMatrixData && confusionMatrixData.length" style="margin-top: 20px">
                <h4>混淆矩阵</h4>
                <v-chart :option="confusionMatrixOption" autoresize style="height: 400px"/>
              </div>
            </el-col>
          </el-row>
        </template>
      </div>
    </el-card>

    <!-- 创建训练任务对话框 -->
    <el-dialog
        v-model="createDialog.visible"
        title="创建训练任务"
        width="800px"
        :close-on-click-modal="false"
    >
      <el-form
          ref="createFormRef"
          :model="createDialog.form"
          :rules="createDialog.rules"
          label-width="140px"
      >
        <!-- 基础配置 -->
        <el-divider content-position="left">基础配置</el-divider>

        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="createDialog.form.taskName" placeholder="请输入任务名称"/>
        </el-form-item>

        <el-form-item label="数据集" prop="datasetId">
          <el-select v-model="createDialog.form.datasetId" placeholder="请选择数据集" style="width: 100%">
            <el-option
                v-for="d in datasets"
                :key="d.datasetId"
                :label="`${d.datasetName} (${d.numSamples} samples)`"
                :value="d.datasetId"
            />
          </el-select>
        </el-form-item>

        <!-- 模型配置 -->
        <el-divider content-position="left">模型配置</el-divider>

        <el-form-item label="模型类型" prop="modelType">
          <el-select v-model="createDialog.form.modelType" placeholder="请选择模型类型" style="width: 100%">
            <el-option label="基础CNN" value="CNN">
              <span>基础CNN</span>
              <span style="float: right; color: #8492a6; font-size: 13px">适合简单任务</span>
            </el-option>
            <el-option label="高级CNN" value="ADVANCED_CNN">
              <span>高级CNN</span>
              <span style="float: right; color: #8492a6; font-size: 13px">带批归一化</span>
            </el-option>
            <el-option label="ResNet" value="RESNET">
              <span>ResNet</span>
              <span style="float: right; color: #8492a6; font-size: 13px">残差网络，深层效果好</span>
            </el-option>
            <el-option label="VGG" value="VGG">
              <span>VGG</span>
              <span style="float: right; color: #8492a6; font-size: 13px">经典深度网络</span>
            </el-option>
            <el-option label="MobileNet" value="MOBILENET">
              <span>MobileNet</span>
              <span style="float: right; color: #8492a6; font-size: 13px">轻量级，速度快</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="隐藏层大小" prop="hiddenSize">
              <el-input-number
                  v-model="createDialog.form.hiddenSize"
                  :min="32"
                  :max="2048"
                  :step="32"
                  style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="激活函数" prop="activation">
              <el-select v-model="createDialog.form.activation" placeholder="请选择激活函数" style="width: 100%">
                <el-option label="ReLU" value="relu"/>
                <el-option label="LeakyReLU" value="leaky_relu"/>
                <el-option label="ELU" value="elu"/>
                <el-option label="Sigmoid" value="sigmoid"/>
                <el-option label="Tanh" value="tanh"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Dropout率" prop="dropout">
              <el-select v-model="createDialog.form.dropout" placeholder="请选择Dropout率" style="width: 100%">
                <el-option label="0.0 (不使用)" value="0.0"/>
                <el-option label="0.1" value="0.1"/>
                <el-option label="0.2" value="0.2"/>
                <el-option label="0.3" value="0.3"/>
                <el-option label="0.4" value="0.4"/>
                <el-option label="0.5" value="0.5"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="批归一化" prop="useBatchNorm">
              <el-switch v-model="createDialog.form.useBatchNorm"/>
              <span style="margin-left: 10px; color: #909399; font-size: 12px">提高训练稳定性</span>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 训练配置 -->
        <el-divider content-position="left">训练配置</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="训练轮数" prop="totalEpochs">
              <el-input-number
                  v-model="createDialog.form.totalEpochs"
                  :min="1"
                  :max="500"
                  style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="批次大小" prop="batchSize">
              <el-select v-model="createDialog.form.batchSize" placeholder="请选择批次大小" style="width: 100%">
                <el-option label="8" :value="8"/>
                <el-option label="16" :value="16"/>
                <el-option label="32" :value="32"/>
                <el-option label="64" :value="64"/>
                <el-option label="128" :value="128"/>
                <el-option label="256" :value="256"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学习率" prop="learningRate">
              <el-select v-model="createDialog.form.learningRate" placeholder="请选择学习率" style="width: 100%">
                <el-option label="0.00001" value="0.00001"/>
                <el-option label="0.0001" value="0.0001"/>
                <el-option label="0.0005" value="0.0005"/>
                <el-option label="0.001" value="0.001"/>
                <el-option label="0.005" value="0.005"/>
                <el-option label="0.01" value="0.01"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优化器" prop="optimizer">
              <el-select v-model="createDialog.form.optimizer" placeholder="请选择优化器" style="width: 100%">
                <el-option label="Adam" value="adam"/>
                <el-option label="AdamW" value="adamw"/>
                <el-option label="SGD" value="sgd"/>
                <el-option label="RMSprop" value="rmsprop"/>
                <el-option label="Nadam" value="nadam"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 高级配置 -->
        <el-divider content-position="left">高级配置</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="损失函数" prop="lossFunction">
              <el-select v-model="createDialog.form.lossFunction" placeholder="请选择损失函数" style="width: 100%">
                <el-option label="Categorical Crossentropy" value="categorical_crossentropy"/>
                <el-option label="Binary Crossentropy" value="binary_crossentropy"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="验证集比例" prop="validationSplit">
              <el-select v-model="createDialog.form.validationSplit" placeholder="请选择验证集比例" style="width: 100%">
                <el-option label="10%" value="0.1"/>
                <el-option label="15%" value="0.15"/>
                <el-option label="20%" value="0.2"/>
                <el-option label="25%" value="0.25"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="L2正则化系数" prop="l2Regularization">
              <el-select v-model="createDialog.form.l2Regularization" placeholder="请选择L2正则化" style="width: 100%">
                <el-option label="0.0 (不使用)" value="0.0"/>
                <el-option label="0.0001" value="0.0001"/>
                <el-option label="0.001" value="0.001"/>
                <el-option label="0.01" value="0.01"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="早停轮数" prop="earlyStoppingPatience">
              <el-input-number
                  v-model="createDialog.form.earlyStoppingPatience"
                  :min="0"
                  :max="50"
                  style="width: 100%"
                  placeholder="0表示不使用"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学习率衰减策略" prop="lrScheduler">
              <el-select v-model="createDialog.form.lrScheduler" placeholder="请选择学习率衰减" style="width: 100%">
                <el-option label="不使用" value="none"/>
                <el-option label="指数衰减" value="exponential"/>
                <el-option label="余弦退火" value="cosine"/>
                <el-option label="阶梯衰减" value="step"/>
                <el-option label="性能衰减" value="reduce_on_plateau"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据增强" prop="useAugmentation">
              <el-switch v-model="createDialog.form.useAugmentation"/>
              <span style="margin-left: 10px; color: #909399; font-size: 12px">增加数据多样性</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="数据增强强度" prop="augmentationStrength" v-if="createDialog.form.useAugmentation">
          <el-radio-group v-model="createDialog.form.augmentationStrength">
            <el-radio label="light">轻度</el-radio>
            <el-radio label="medium">中度</el-radio>
            <el-radio label="strong">强度</el-radio>
          </el-radio-group>
        </el-form-item>

      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateTask" :loading="createDialog.loading">
            创建
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 任务详情对话框 -->
    <el-dialog
        v-model="detailDialog.visible"
        title="训练任务详情"
        width="1100px"
        :close-on-click-modal="false"
    >
      <el-descriptions :column="2" border v-if="detailDialog.task">
        <el-descriptions-item label="任务ID">
          {{ detailDialog.task.taskId }}
        </el-descriptions-item>

        <el-descriptions-item label="任务名称">
          {{ detailDialog.task.taskName }}
        </el-descriptions-item>

        <el-descriptions-item label="模型类型">
          {{ detailDialog.task.trainingConfigParsed.modeltype }}
        </el-descriptions-item>

        <el-descriptions-item label="训练轮数">
          {{ detailDialog.task.trainingConfigParsed.epochs }}
        </el-descriptions-item>

        <el-descriptions-item label="批次大小">
          {{ detailDialog.task.trainingConfigParsed.batchsize }}
        </el-descriptions-item>

        <el-descriptions-item label="学习率">
          {{ detailDialog.task.trainingConfigParsed.learningrate }}
        </el-descriptions-item>

        <el-descriptions-item label="优化器">
          {{ detailDialog.task.trainingConfigParsed.optimizer }}
        </el-descriptions-item>

        <el-descriptions-item label="激活函数">
          {{ detailDialog.task.trainingConfigParsed.activation }}
        </el-descriptions-item>

        <el-descriptions-item label="Dropout">
          {{ detailDialog.task.trainingConfigParsed.dropout }}
        </el-descriptions-item>

        <el-descriptions-item label="数据增强">
          {{ detailDialog.task.trainingConfigParsed.useAugmentation ? '是' : '否' }}
        </el-descriptions-item>

        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailDialog.task.status)">
            {{ getStatusText(detailDialog.task.status) }}
          </el-tag>
        </el-descriptions-item>

        <el-descriptions-item label="训练进度">
          {{ detailDialog.task.progress }}%
        </el-descriptions-item>

        <el-descriptions-item label="当前轮次">
          {{ detailDialog.task.currentEpoch || 0 }} / {{ detailDialog.task.totalEpochs }}
        </el-descriptions-item>

        <el-descriptions-item label="训练集最佳准确率">
          {{ detailDialog.task.bestAccuracy ? (detailDialog.task.bestAccuracy * 100).toFixed(2) + '%' : '-' }}
        </el-descriptions-item>

        <el-descriptions-item label="最终准确率">
          {{ detailDialog.task.finalAccuracy ? (detailDialog.task.finalAccuracy * 100).toFixed(2) + '%' : '-' }}
        </el-descriptions-item>

        <el-descriptions-item label="最终损失">
          {{ detailDialog.task.finalLoss ? detailDialog.task.finalLoss.toFixed(6) : '-' }}
        </el-descriptions-item>

        <el-descriptions-item label="开始时间">
          {{ formatDate(detailDialog.task.startTime) }}
        </el-descriptions-item>

        <el-descriptions-item label="结束时间">
          {{ formatDate(detailDialog.task.endTime) }}
        </el-descriptions-item>

        <el-descriptions-item label="创建时间">
          {{ formatDate(detailDialog.task.createTime) }}
        </el-descriptions-item>

        <el-descriptions-item label="更新时间">
          {{ formatDate(detailDialog.task.updateTime) }}
        </el-descriptions-item>

        <el-descriptions-item label="错误信息" :span="2" v-if="detailDialog.task.errorMessage">
          <el-text type="danger">{{ detailDialog.task.errorMessage }}</el-text>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 混淆矩阵区域 -->
      <div v-if="confusionMatrixData && confusionMatrixData.length" style="margin-top: 20px">
        <h4>混淆矩阵</h4>
        <v-chart :option="confusionMatrixOption" autoresize style="height: 400px"/>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialog.visible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 训练日志对话框 -->
    <el-dialog
        v-model="logsDialog.visible"
        title="训练日志"
        width="1000px"
        :close-on-click-modal="false"
    >
      <div class="charts-container" v-loading="logsDialog.loading">
        <div v-if="!logsDialog.logs.length" class="no-logs-tip">
          暂无训练日志。
        </div>
        <template v-else>
          <div class="logs-summary">
            <span>最新 Epoch：{{ latestLog?.epoch ?? '-' }}</span>
            <span>最新 Step：{{ latestLog?.step ?? '-' }}</span>
            <span>当前学习率：{{ latestLog?.learningRate ?? '-' }}</span>
            <span>Batch Size：{{ latestLog?.batchSize ?? '-' }}</span>
            <span>最近日志时间：{{ latestLog ? formatDate(latestLog.timestamp) : '-' }}</span>
          </div>

          <div class="chart-item">
            <h4>准确率曲线</h4>
            <v-chart :option="accuracyChartOption" autoresize style="height: 300px"/>
          </div>

          <div class="chart-item">
            <h4>损失曲线</h4>
            <v-chart :option="lossChartOption" autoresize style="height: 300px"/>
          </div>

          <div class="chart-item">
            <h4>学习率曲线</h4>
            <v-chart :option="lrChartOption" autoresize style="height: 300px"/>
          </div>

          <div class="chart-item">
            <h4>准确率差（过拟合观察）</h4>
            <v-chart :option="gapAccChartOption" autoresize style="height: 300px"/>
          </div>

          <div class="chart-item">
            <h4>每个 Epoch 时长</h4>
            <v-chart :option="epochDurationChartOption" autoresize style="height: 300px"/>
          </div>

          <div v-if="confusionMatrixData && confusionMatrixData.length" style="margin-top: 20px">
            <h4>混淆矩阵</h4>
            <v-chart :option="confusionMatrixOption" autoresize style="height: 400px"/>
          </div>
        </template>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="logsDialog.visible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted, computed} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {HeatmapChart} from 'echarts/charts'
import {VisualMapComponent} from 'echarts/components'
import {
  getTrainingTaskList,
  createTrainingTask,
  getTrainingTaskDetail,
  getTrainingLogs,
  cancelTrainingTask
} from '@/api/training'
import {
  DataAnalysis,
  CircleCheck,
  Loading,
  TrendCharts,
  Search,
  Refresh,
  Plus,
  View,
  VideoPause,
  Document
} from '@element-plus/icons-vue'
import {use} from 'echarts/core'
import {CanvasRenderer} from 'echarts/renderers'
import {LineChart} from 'echarts/charts'
import {BarChart} from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'
import {getAvailableDatasets} from '@/api/dataset'


// 注册ECharts组件
use([
  CanvasRenderer,
  LineChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  HeatmapChart,
  VisualMapComponent
])

// 数据
const loading = ref(false)
const inlineLogsLoading = ref(false)
const datasets = ref([])
const taskList = ref([])
const statistics = ref({
  totalTasks: 0,
  completedTasks: 0,
  runningTasks: 0,
  avgAccuracy: 0
})

const selectedTask = ref(null)

const filterForm = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const createDialog = reactive({
  visible: false,
  loading: false,
  form: {
    taskName: '',
    datasetId: null,
    modelType: 'CNN',
    totalEpochs: 10,
    batchSize: 32,
    learningRate: '0.001',
    optimizer: 'adam',
    lossFunction: 'categorical_crossentropy',
    activation: 'relu',
    dropout: '0.2',
    hiddenSize: 128,
    validationSplit: '0.2',
    useAugmentation: false,
    augmentationStrength: 'medium',
    useBatchNorm: true,
    l2Regularization: '0.0',
    earlyStoppingPatience: 5,
    lrScheduler: 'none'
  },
  rules: {
    taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
    datasetId: [{ required: true, message: '请选择数据集', trigger: 'change' }],
    modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
    totalEpochs: [{ required: true, message: '请输入训练轮数', trigger: 'blur' }],
    batchSize: [{ required: true, message: '请选择批次大小', trigger: 'change' }],
    learningRate: [{ required: true, message: '请选择学习率', trigger: 'change' }],
    optimizer: [{ required: true, message: '请选择优化器', trigger: 'change' }]
  }
})

const loadDatasets = async () => {
  try {
    const res = await getAvailableDatasets()
    if (res.code === 200) {
      datasets.value = res.data
    }
  } catch (err) {
    console.error('加载数据集失败', err)
    ElMessage.error('加载数据集失败')
  }
}

const createFormRef = ref()

const detailDialog = reactive({
  visible: false,
  task: null
})

const logsDialog = reactive({
  visible: false,
  loading: false,
  logs: []
})

const latestLog = computed(() => {
  if (!logsDialog.logs || !logsDialog.logs.length) return null
  return logsDialog.logs[logsDialog.logs.length - 1]
})

// 图表配置
const accuracyChartOption = computed(() => {
  const epochs = logsDialog.logs.map(log => log.epoch)
  const trainAcc = logsDialog.logs.map(log => parseFloat((log.accuracy * 100).toFixed(2)))
  const valAcc = logsDialog.logs.map(log => parseFloat((log.valAccuracy * 100).toFixed(2)))

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: {color: '#606266'}
    },
    legend: {
      data: ['训练准确率', '验证准确率'],
      bottom: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: epochs,
      name: 'Epoch',
      axisLine: {lineStyle: {color: '#e4e7ed'}},
      axisLabel: {color: '#909399'}
    },
    yAxis: {
      type: 'value',
      name: '准确率 (%)',
      axisLine: {lineStyle: {color: '#e4e7ed'}},
      axisLabel: {color: '#909399'},
      splitLine: {lineStyle: {color: '#f5f7fa'}}
    },
    series: [
      {
        name: '训练准确率',
        type: 'line',
        data: trainAcc,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {width: 3, color: '#409EFF'},
        itemStyle: {color: '#409EFF'}
      },
      {
        name: '验证准确率',
        type: 'line',
        data: valAcc,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {width: 3, color: '#67C23A'},
        itemStyle: {color: '#67C23A'}
      }
    ]
  }
})

const lossChartOption = computed(() => {
  const epochs = logsDialog.logs.map(log => log.epoch)
  const trainLoss = logsDialog.logs.map(log => parseFloat(log.loss.toFixed(4)))
  const valLoss = logsDialog.logs.map(log => parseFloat(log.valLoss.toFixed(4)))

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: {color: '#606266'}
    },
    legend: {
      data: ['训练损失', '验证损失'],
      bottom: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: epochs,
      name: 'Epoch',
      axisLine: {lineStyle: {color: '#e4e7ed'}},
      axisLabel: {color: '#909399'}
    },
    yAxis: {
      type: 'value',
      name: '损失',
      axisLine: {lineStyle: {color: '#e4e7ed'}},
      axisLabel: {color: '#909399'},
      splitLine: {lineStyle: {color: '#f5f7fa'}}
    },
    series: [
      {
        name: '训练损失',
        type: 'line',
        data: trainLoss,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {width: 3, color: '#E6A23C'},
        itemStyle: {color: '#E6A23C'}
      },
      {
        name: '验证损失',
        type: 'line',
        data: valLoss,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {width: 3, color: '#F56C6C'},
        itemStyle: {color: '#F56C6C'}
      }
    ]
  }
})

const lrChartOption = computed(() => {
  const epochs = logsDialog.logs.map(log => log.epoch)
  const lrs = logsDialog.logs.map(log =>
      log.learningRate != null ? Number(log.learningRate) : null
  )

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: {color: '#606266'}
    },
    legend: {
      data: ['学习率'],
      bottom: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: epochs,
      name: 'Epoch',
      axisLine: {lineStyle: {color: '#e4e7ed'}},
      axisLabel: {color: '#909399'}
    },
    yAxis: {
      type: 'value',
      name: '学习率',
      axisLine: {lineStyle: {color: '#e4e7ed'}},
      axisLabel: {color: '#909399'},
      splitLine: {lineStyle: {color: '#f5f7fa'}},
    },
    series: [
      {
        name: '学习率',
        type: 'line',
        data: lrs,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {width: 3, color: '#909399'},
        itemStyle: {color: '#909399'}
      }
    ]
  }
})

const gapAccChartOption = computed(() => {
  const epochs = logsDialog.logs.map(log => log.epoch)
  const gaps = logsDialog.logs.map(log => {
    if (log.accuracy != null && log.valAccuracy != null) {
      return parseFloat(((Number(log.valAccuracy) - Number(log.accuracy)) * 100).toFixed(2))
    }
    return null
  })

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: {color: '#606266'}
    },
    legend: {
      data: ['验证-训练 准确率差'],
      bottom: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: epochs,
      name: 'Epoch',
      axisLine: {lineStyle: {color: '#e4e7ed'}},
      axisLabel: {color: '#909399'}
    },
    yAxis: {
      type: 'value',
      name: '差值 (%)',
      axisLine: {lineStyle: {color: '#e4e7ed'}},
      axisLabel: {color: '#909399'},
      splitLine: {lineStyle: {color: '#f5f7fa'}}
    },
    series: [
      {
        name: '验证-训练 准确率差',
        type: 'line',
        data: gaps,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {width: 3, color: '#F56C6C'},
        itemStyle: {color: '#F56C6C'}
      }
    ]
  }
})

const epochDurationChartOption = computed(() => {
  if (!logsDialog.logs.length) return {}

  const epochs = logsDialog.logs.map(log => log.epoch)
  const durations = logsDialog.logs.map((log, index, arr) => {
    if (index === 0) return null
    const prev = arr[index - 1]
    if (!prev.timestamp || !log.timestamp) return null
    const t1 = new Date(prev.timestamp).getTime()
    const t2 = new Date(log.timestamp).getTime()
    return parseFloat(((t2 - t1) / 1000).toFixed(2)) // 秒
  })

  return {
    tooltip: {trigger: 'axis'},
    legend: {bottom: 10},
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: epochs,
      name: 'Epoch'
    },
    yAxis: {
      type: 'value',
      name: '秒'
    },
    series: [
      {
        name: 'Epoch 时长 (秒)',
        type: 'bar',
        data: durations,
        itemStyle: {color: '#67C23A'}
      }
    ]
  }
})

const confusionMatrixData = computed(() => {
  if (!detailDialog.task || !detailDialog.task.confusionMatrixJson) return null
  try {
    return JSON.parse(detailDialog.task.confusionMatrixJson) // 2D array
  } catch (e) {
    console.error('解析 confusionMatrixJson 失败', e)
    return null
  }
})

const confusionClassNames = computed(() => {
  if (!detailDialog.task || !detailDialog.task.classNamesJson) return null
  try {
    return JSON.parse(detailDialog.task.classNamesJson) // array of strings
  } catch (e) {
    console.error('解析 classNamesJson 失败', e)
    return null
  }
})

const confusionMatrixOption = computed(() => {
  const cm = confusionMatrixData.value
  const labels = confusionClassNames.value

  if (!cm || !cm.length) return {}

  const numClasses = cm.length
  // 如果没提供 classNames，用 0..n-1 代替
  const axisLabels =
      labels && labels.length === numClasses
          ? labels
          : Array.from({length: numClasses}, (_, i) => String(i))

  // 把二维数组转成 [x, y, value] 格式
  const data = []
  let maxValue = 0
  for (let i = 0; i < numClasses; i++) {
    for (let j = 0; j < numClasses; j++) {
      const v = cm[i][j] || 0
      data.push([j, i, v]) // x: 预测, y: 真实
      if (v > maxValue) maxValue = v
    }
  }

  return {
    tooltip: {
      position: 'top',
      formatter: params => {
        const real = axisLabels[params.value[1]]
        const pred = axisLabels[params.value[0]]
        const count = params.value[2]
        return `真实: ${real}<br/>预测: ${pred}<br/>样本数: ${count}`
      }
    },
    grid: {
      left: '10%',
      right: '10%',
      top: '10%',
      bottom: '25%'
    },
    xAxis: {
      type: 'category',
      data: axisLabels,
      name: '预测类别',
      axisLabel: {rotate: 45}
    },
    yAxis: {
      type: 'category',
      data: axisLabels,
      name: '真实类别'
    },
    visualMap: {
      min: 0,
      max: maxValue || 1,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0',
      inRange: {
        color: ['#f5f5f5', '#409EFF'] // 颜色从浅到深
      }
    },
    series: [
      {
        name: '混淆矩阵',
        type: 'heatmap',
        data,
        label: {
          show: true,
          formatter: params => params.value[2] || ''
        }
      }
    ]
  }
})

const loadLogsForSelectedTask = async () => {
  if (!selectedTask.value) return
  inlineLogsLoading.value = true
  try {
    const response = await getTrainingLogs(selectedTask.value.taskId)
    if (response.code === 200) {
      logsDialog.logs = response.data || []
    }
  } catch (error) {
    console.error('获取训练日志失败', error)
    ElMessage.error('获取训练日志失败')
  } finally {
    inlineLogsLoading.value = false
  }
}

const updateStatistics = () => {
  statistics.value.totalTasks = taskList.value.length
  statistics.value.completedTasks = taskList.value.filter(t => t.status === 'COMPLETED').length
  statistics.value.runningTasks = taskList.value.filter(t => t.status === 'RUNNING').length

  const completedTasks = taskList.value.filter(t => t.finalAccuracy)
  if (completedTasks.length > 0) {
    const totalAcc = completedTasks.reduce((sum, t) => sum + t.finalAccuracy, 0)
    statistics.value.avgAccuracy = totalAcc / completedTasks.length
  }
}

const showCreateDialog = () => {
  createDialog.visible = true
  createDialog.form = {
    taskName: '',
    datasetId: null,
    modelType: 'CNN',
    totalEpochs: 10,
    batchSize: 32,
    learningRate: '0.001',
    optimizer: 'adam',
    lossFunction: 'categorical_crossentropy',
    activation: 'relu',
    dropout: '0.2',
    hiddenSize: 128,
    validationSplit: '0.2',
    useAugmentation: false,
    augmentationStrength: 'medium',
    useBatchNorm: true,
    l2Regularization: '0.0',
    earlyStoppingPatience: 5,
    lrScheduler: 'none'
  }
}

const handleCreateTask = async () => {
  try {
    await createFormRef.value.validate()
    createDialog.loading = true

    const res = await createTrainingTask(createDialog.form)

    if (res.code === 200) {
      ElMessage.success('训练任务创建成功')
      createDialog.visible = false
      loadTaskList()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error) {
    console.error('创建任务失败', error)
    if (error !== false) {
      ElMessage.error('创建任务失败')
    }
  } finally {
    createDialog.loading = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadTaskList()
}

const resetFilter = () => {
  filterForm.keyword = ''
  filterForm.status = ''
  pagination.current = 1
  loadTaskList()
}

const loadTaskList = async () => {
  try {
    loading.value = true
    const params = {
      current: pagination.current,
      size: pagination.size,
      status: filterForm.status || undefined
    }

    const res = await getTrainingTaskList(params)

    if (res.code === 200) {
      taskList.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    console.error('加载任务列表失败', error)
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  loadTaskList()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  loadTaskList()
}

const handleRowClick = (row) => {
  selectedTask.value = row
  loadInlineLogs(row.taskId)
}

const loadInlineLogs = async (taskId) => {
  try {
    inlineLogsLoading.value = true
    const res = await getTrainingLogs(taskId)

    if (res.code === 200) {
      logsDialog.logs = res.data
    }
  } catch (error) {
    console.error('加载日志失败', error)
  } finally {
    inlineLogsLoading.value = false
  }
}

const viewDetail = async (row) => {
  try {
    const res = await getTrainingTaskDetail(row.taskId)

    if (res.code === 200) {
      detailDialog.task = res.data

      // 解析训练配置
      try {
        detailDialog.task.trainingConfigParsed = JSON.parse(res.data.trainingConfig)
      } catch (e) {
        detailDialog.task.trainingConfigParsed = {}
      }

      detailDialog.visible = true
    }
  } catch (error) {
    console.error('加载详情失败', error)
    ElMessage.error('加载详情失败')
  }
}

const openLogsDialog = async (row) => {
  try {
    logsDialog.loading = true
    logsDialog.visible = true

    const res = await getTrainingLogs(row.taskId)

    if (res.code === 200) {
      logsDialog.logs = res.data
    }
  } catch (error) {
    console.error('加载日志失败', error)
    ElMessage.error('加载日志失败')
  } finally {
    logsDialog.loading = false
  }
}

const handleCancelTask = (row) => {
  ElMessageBox.confirm(
      '确定要取消该训练任务吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      const res = await cancelTrainingTask(row.taskId)

      if (res.code === 200) {
        ElMessage.success('任务已取消')
        loadTaskList()
      } else {
        ElMessage.error(res.message || '取消失败')
      }
    } catch (error) {
      console.error('取消任务失败', error)
      ElMessage.error('取消任务失败')
    }
  }).catch(() => {})
}

const getStatusType = (status) => {
  const types = {
    PENDING: 'info',
    RUNNING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger',
    CANCELLED: 'info'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    PENDING: '等待中',
    RUNNING: '训练中',
    COMPLETED: '已完成',
    FAILED: '已失败',
    CANCELLED: '已取消'
  }
  return texts[status] || status
}

const formatDate = (date) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const tableRowClassName = ({ row }) => {
  if (selectedTask.value && row.taskId === selectedTask.value.taskId) {
    return 'selected-row'
  }
  return ''
}

// 页面加载时执行
onMounted(() => {
  loadDatasets()
  loadTaskList()

  // 定时刷新正在运行的任务
  const interval = setInterval(() => {
    if (taskList.value.some(t => t.status === 'RUNNING')) {
      loadTaskList()
      if (selectedTask.value && selectedTask.value.status === 'RUNNING') {
        loadInlineLogs(selectedTask.value.taskId)
      }
    }
  }, 3000)
})
</script>

<style lang="scss" scoped>
.training-management {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  :deep(.el-card__body) {
    padding: 20px;
  }

  .stat-content {
    display: flex;
    align-items: center;

    .stat-icon {
      font-size: 40px;
      margin-right: 20px;
      color: #409EFF;
    }

    .stat-info {
      flex: 1;

      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
        margin-bottom: 4px;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
      }
    }
  }

  &.primary .stat-icon {
    color: #409EFF;
  }

  &.success .stat-icon {
    color: #67C23A;
  }

  &.warning .stat-icon {
    color: #E6A23C;
  }

  &.info .stat-icon {
    color: #909399;
  }
}

.filter-card, .table-card {
  margin-bottom: 20px;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.selected-task-card {
  margin-top: 20px;
  background: #f5f7fa;
}

.selected-task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #e4e7ed;
}

.selected-task-meta {
  display: flex;
  gap: 20px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.selected-task-meta span {
  color: #606266;
  font-size: 14px;
}

.selected-task-time {
  text-align: right;
  color: #909399;
  font-size: 13px;
}

.charts-container {
  min-height: 200px;
}

.no-logs-tip {
  text-align: center;
  padding: 40px;
  color: #909399;
  font-size: 14px;
}

.logs-summary {
  display: flex;
  justify-content: space-around;
  padding: 15px;
  background: #ecf5ff;
  border-radius: 4px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.logs-summary span {
  color: #409eff;
  font-weight: 500;
  font-size: 14px;
}

.chart-item {
  margin-bottom: 20px;
}

.chart-item h4 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 16px;
}

:deep(.el-table .selected-row) {
  background-color: #ecf5ff !important;
}

:deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}

:deep(.el-divider__text) {
  font-weight: 600;
  color: #409eff;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}
</style>
