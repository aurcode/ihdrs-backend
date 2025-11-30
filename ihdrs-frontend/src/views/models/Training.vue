// views/models/Training.vue

<template>
  <div class="training-management">
    <!-- 动态背景粒子效果 -->
    <div class="background-particles">
      <div v-for="i in 20" :key="i" class="particle" :style="getParticleStyle(i)"></div>
    </div>

    <!-- 背景装饰圆形 -->
    <div class="background-circles">
      <div class="circle circle-top"></div>
      <div class="circle circle-bottom"></div>
      <div class="circle circle-middle"></div>
    </div>

    <!-- 内容区域 -->
    <div class="content-wrapper">
      <!-- 头部Logo区域 -->
      <div class="header-section">
        <div class="logo-container">
          <div class="logo-circle">
            <el-icon size="50" color="#2563eb">
              <Loading />
            </el-icon>
          </div>
        </div>
        <h1 class="header-title">训练任务管理</h1>
        <p class="header-subtitle">Training Task Management</p>
      </div>

      <!-- 搜索筛选区域 -->
      <el-card class="search-card modern-card" shadow="hover">
        <el-form :model="filterForm" inline>
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

      <!-- 顶部统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card primary">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <DataAnalysis/>
                </el-icon>
              </div>
              <div class="stat-label">总任务数</div>
              <div class="stat-value">{{ statistics.totalTasks || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card success">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <CircleCheck/>
                </el-icon>
              </div>
              <div class="stat-label">已完成</div>
              <div class="stat-value">{{ statistics.completedTasks || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card warning">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <Loading/>
                </el-icon>
              </div>
              <div class="stat-label">训练中</div>
              <div class="stat-value">{{ statistics.runningTasks || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card danger">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <TrendCharts/>
                </el-icon>
              </div>
              <div class="stat-label">平均准确率</div>
              <div class="stat-value">{{ (statistics.avgAccuracy * 100).toFixed(2) }}%</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 任务列表 -->
      <el-card class="table-card modern-card" shadow="hover">
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
              <span>提示：混淆矩阵将在训练完成后自动生成</span>
            </div>
          </div>
          <div class="selected-task-time">
            <div>开始时间：{{ formatDate(selectedTask.startTime) }}</div>
            <div>结束时间：{{ formatDate(selectedTask.endTime) }}</div>
          </div>
        </div>

        <!-- 实时进度条 -->
        <div v-if="selectedTask.status === 'RUNNING'" class="real-time-progress">
          <div class="progress-header">
            <span class="progress-epoch">Epoch {{ batchProgress.epoch }}/{{ batchProgress.totalEpochs }}</span>
            <span class="progress-speed" v-if="batchProgress.msPerStep > 0">
      {{ batchProgress.msPerStep }}ms/step
    </span>
          </div>

          <div class="progress-bar-container">
            <span class="progress-counter">{{ batchProgress.currentBatch }}/{{ batchProgress.totalBatches }}</span>
            <div class="progress-bar-wrapper">
              <div class="progress-bar-track">
                <div
                    class="progress-bar-fill"
                    :style="{ width: batchProgressPercent + '%' }"
                >
                </div>
              </div>
              <span class="progress-bar-text">{{ batchProgressBar }}</span>
            </div>
          </div>
        </div>

        <div class="charts-container" v-loading="inlineLogsLoading">
          <div v-if="!logsDialog.logs.length" class="no-logs-tip">
            暂无训练日志，请等待训练过程中产生日志。
          </div>
          <template v-else>
            <!-- 终端输出 -->
            <div class="chart-item" style="position: relative">
              <h4>
                终端输出
                <el-button
                    size="small"
                    text
                    @click="scrollToBottom"
                    style="position: absolute; right: 10px; top: 0"
                >
                  跳到最新 ↓
                </el-button>
              </h4>
              <el-scrollbar ref="terminalScrollbar" height="250px">
                <pre class="terminal-log">{{ terminalLogsText }}</pre>
              </el-scrollbar>
            </div>
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
                  <h4>
                    混淆矩阵
                    <el-tooltip
                        content="混淆矩阵用于评估分类模型，通过对比预测与真实标签，展示正确和错误分类的数量，让你直观看到模型在哪些类别上表现良好或存在误判"
                        placement="top">
                      <el-icon>
                        <QuestionFilled/>
                      </el-icon>
                    </el-tooltip>
                  </h4>
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

          <el-form-item prop="taskName">
            <template #label>
              <span>任务名称</span>
              <el-tooltip content="为训练任务设置一个便于识别的名称" placement="top">
                <el-icon style="margin-left: 4px; cursor: help;">
                  <QuestionFilled/>
                </el-icon>
              </el-tooltip>
            </template>
            <el-input v-model="createDialog.form.taskName" placeholder="请输入任务名称"/>
          </el-form-item>

          <el-form-item label="数据集" prop="datasetId">
            <template #label>
              <span>数据集</span>
              <el-tooltip content="选择用于训练的数据集，数据集必须已经上传并预处理完成" placement="top">
                <el-icon style="margin-left: 4px; cursor: help;">
                  <QuestionFilled/>
                </el-icon>
              </el-tooltip>
            </template>
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
            <template #label>
              <span>模型类型</span>
              <el-tooltip content="选择神经网络架构类型，不同模型适合不同复杂度的任务" placement="top">
                <el-icon style="margin-left: 4px; cursor: help;">
                  <QuestionFilled/>
                </el-icon>
              </el-tooltip>
            </template>
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
                <template #label>
                  <span>隐藏层大小</span>
                  <el-tooltip content="全连接层的神经元数量，越大模型容量越大，但训练时间更长" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
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
                <template #label>
                  <span>激活函数</span>
                  <el-tooltip content="神经元激活函数，ReLU 最常用，LeakyReLU 避免神经元死亡" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
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
                <template #label>
                  <span>Dropout率</span>
                  <el-tooltip content="随机丢弃神经元的比例，用于防止过拟合，0.0 表示不使用" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
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
                <template #label>
                  <span>批归一化</span>
                  <el-tooltip content="标准化每一层的输入，提高训练稳定性和收敛速度" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
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
                <template #label>
                  <span>训练轮数</span>
                  <el-tooltip content="模型遍历整个训练集的次数，越多效果可能越好但容易过拟合" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
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
                <template #label>
                  <span>批次大小</span>
                  <el-tooltip content="每次训练使用的样本数量，越大训练越稳定但显存占用越高" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
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
                <template #label>
                  <span>学习率</span>
                  <el-tooltip content="控制参数更新的步长，过大可能不收敛，过小训练太慢" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
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
                <template #label>
                  <span>优化器</span>
                  <el-tooltip content="参数更新算法，Adam 自适应学习率最常用，SGD 需要精细调参" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
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
                <template #label>
                  <span>损失函数</span>
                  <el-tooltip content="用于计算预测值与真实值的差异，多分类用 Categorical Crossentropy" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
                <el-select v-model="createDialog.form.lossFunction" placeholder="请选择损失函数" style="width: 100%">
                  <el-option label="Categorical Crossentropy" value="categorical_crossentropy"/>
                  <el-option label="Binary Crossentropy" value="binary_crossentropy"/>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="验证集比例" prop="validationSplit">
                <template #label>
                  <span>验证集比例</span>
                  <el-tooltip content="从训练集中划分出用于验证的数据比例，用于监控过拟合" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
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
                <template #label>
                  <span>L2正则化系数</span>
                  <el-tooltip content="权重衰减系数，防止模型过拟合，值越大约束越强" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
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
                <template #label>
                  <span>早停轮数</span>
                  <el-tooltip content="验证损失连续多少轮不下降时停止训练，0 表示不使用早停" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
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
                <template #label>
                  <span>学习率衰减策略</span>
                  <el-tooltip content="训练过程中动态调整学习率的策略，有助于模型更好收敛" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
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
                <template #label>
                  <span>数据增强</span>
                  <el-tooltip content="对图像进行随机变换（旋转、翻转等），增加数据多样性，防止过拟合" placement="top">
                    <el-icon style="margin-left: 4px; cursor: help;">
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </template>
                <el-switch v-model="createDialog.form.useAugmentation"/>
                <span style="margin-left: 10px; color: #909399; font-size: 12px">增加数据多样性</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="数据增强强度" prop="augmentationStrength" v-if="createDialog.form.useAugmentation">
            <template #label>
              <span>数据增强强度</span>
              <el-tooltip content="数据增强的变换幅度，轻度适合高质量数据，强度适合数据量少的情况" placement="top">
                <el-icon style="margin-left: 4px; cursor: help;">
                  <QuestionFilled/>
                </el-icon>
              </el-tooltip>
            </template>
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
          <h4>
            混淆矩阵
            <el-tooltip
                content="混淆矩阵用于评估分类模型，通过对比预测与真实标签，展示正确和错误分类的数量，让你直观看到模型在哪些类别上表现良好或存在误判"
                placement="top">
              <el-icon>
                <QuestionFilled/>
              </el-icon>
            </el-tooltip>
          </h4>
          <v-chart :option="confusionMatrixOption" autoresize style="height: 400px"/>
        </div>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="detailDialog.visible = false">关闭</el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted, computed, watch, nextTick} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {HeatmapChart} from 'echarts/charts'
import {VisualMapComponent} from 'echarts/components'
import {
  getTrainingTaskList,
  createTrainingTask,
  getTrainingTaskDetail,
  getTrainingLogs,
  cancelTrainingTask,
  getTrainingStatistics
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
  Document,
  QuestionFilled
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
import {getBatchProgress} from '@/api/training'

// 粒子效果样式
const getParticleStyle = (index) => {
  const size = Math.random() * 3 + 1
  const x = Math.random() * 100
  const y = Math.random() * 100
  const duration = Math.random() * 3 + 2
  const delay = Math.random() * 2

  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${x}%`,
    top: `${y}%`,
    animationDuration: `${duration}s`,
    animationDelay: `${delay}s`,
    opacity: Math.random() * 0.3 + 0.1
  }
}

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

// 实时 batch 进度
const batchProgress = reactive({
  epoch: 0,
  totalEpochs: 0,
  currentBatch: 0,
  totalBatches: 0,
  msPerStep: 0,
  status: 'waiting'
})

let batchProgressTimer = null

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
    taskName: [{required: true, message: '请输入任务名称', trigger: 'blur'}],
    datasetId: [{required: true, message: '请选择数据集', trigger: 'change'}],
    modelType: [{required: true, message: '请选择模型类型', trigger: 'change'}],
    totalEpochs: [{required: true, message: '请输入训练轮数', trigger: 'blur'}],
    batchSize: [{required: true, message: '请选择批次大小', trigger: 'change'}],
    learningRate: [{required: true, message: '请选择学习率', trigger: 'change'}],
    optimizer: [{required: true, message: '请选择优化器', trigger: 'change'}]
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

const loadStatistics = async () => {
  try {
    const res = await getTrainingStatistics()
    if (res.code === 200) {
      statistics.value = res. data
    }
  } catch (error) {
    console.error('加载统计信息失败', error)
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

// batch 进度百分比
const batchProgressPercent = computed(() => {
  if (batchProgress.totalBatches === 0) return 0
  return Math.min(100, (batchProgress.currentBatch / batchProgress.totalBatches) * 100)
})

// 进度条字符（绿色方块）
const batchProgressBar = computed(() => {
  const width = 40
  const filled = Math.floor(width * batchProgressPercent.value / 100)
  return '━'.repeat(filled) + '─'.repeat(width - filled)
})

// 开始轮询 batch 进度
const startBatchProgressPolling = () => {
  if (batchProgressTimer) return

  batchProgressTimer = setInterval(async () => {
    if (!selectedTask.value || selectedTask.value.status !== 'RUNNING') {
      stopBatchProgressPolling()
      return
    }

    try {
      const res = await getBatchProgress(selectedTask.value.taskId)
      if (res.code === 200 && res.data) {
        Object.assign(batchProgress, res.data)
      }
    } catch (err) {
      console.error('获取 batch 进度失败', err)
    }
  }, 500) // 每 500ms 更新一次
}

// 停止轮询
const stopBatchProgressPolling = () => {
  if (batchProgressTimer) {
    clearInterval(batchProgressTimer)
    batchProgressTimer = null
  }
}

const latestLog = computed(() => {
  if (!logsDialog.logs || !logsDialog.logs.length) return null
  return logsDialog.logs[logsDialog.logs.length - 1]
})

const terminalScrollbar = ref(null)

const terminalLogsText = computed(() => {
  if (!logsDialog.logs || !logsDialog.logs.length) return ''
  return logsDialog.logs
      .filter((log) => log.message)
      .map((log) => {
        const timeStr = log.timestamp ? formatDate(log.timestamp) : ''
        return timeStr ? `[${timeStr}] ${log.message}` : log.message
      })
      .join('\n\n')
})

// 自动滚动到底部（仅当用户在底部附近时）
watch(
    () => logsDialog.logs,
    async () => {
      await nextTick()
      if (terminalScrollbar.value) {
        const scrollContainer = terminalScrollbar.value.wrapRef
        if (!scrollContainer) return

        const isNearBottom =
            scrollContainer.scrollHeight - scrollContainer.scrollTop - scrollContainer.clientHeight < 50

        if (isNearBottom) {
          scrollContainer.scrollTop = scrollContainer.scrollHeight
        }
      }
    },
    {deep: true}
)

// 手动跳到最新
const scrollToBottom = () => {
  if (terminalScrollbar.value) {
    const scrollContainer = terminalScrollbar.value.wrapRef
    if (scrollContainer) {
      scrollContainer.scrollTop = scrollContainer.scrollHeight
    }
  }
}

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
  const targetTask = detailDialog.visible ? detailDialog.task : selectedTask.value

  if (!targetTask || !targetTask.confusionMatrixJson) return null
  try {
    return JSON.parse(targetTask.confusionMatrixJson)
  } catch (e) {
    console.error('解析 confusionMatrixJson 失败', e)
    return null
  }
})

const confusionClassNames = computed(() => {
  const targetTask = detailDialog.visible ? detailDialog.task : selectedTask.value

  if (!targetTask || !targetTask.classNamesJson) return null
  try {
    return JSON.parse(targetTask.classNamesJson)
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
      status: filterForm.status || undefined,
      keyword: filterForm.keyword || undefined
    }

    const res = await getTrainingTaskList(params)

    if (res.code === 200) {
      taskList.value = res.data.records
      pagination.total = res.data.total
    }
    loadStatistics()
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

  if (row.status === 'RUNNING') {
    startBatchProgressPolling()
  } else {
    stopBatchProgressPolling()
  }
}

const loadInlineLogs = async (taskId) => {
  if (!taskId) return

  try {
    inlineLogsLoading.value = true
    const res = await getTrainingLogs(taskId)
    if (res.code === 200) {
      logsDialog.logs = res.data || []
    }
  } catch (err) {
    console.error('加载训练日志失败', err)
    ElMessage.error('加载训练日志失败')
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
  }).catch(() => {
  })
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

const tableRowClassName = ({row}) => {
  if (selectedTask.value && row.taskId === selectedTask.value.taskId) {
    return 'selected-row'
  }
  return ''
}

// 页面加载时执行
onMounted(() => {
  loadDatasets()
  loadTaskList()
  loadStatistics()

  // 定时刷新正在运行的任务
  const interval = setInterval(() => {
    if (taskList.value.some(t => t.status === 'RUNNING')) {
      loadTaskList()
      if (selectedTask.value) {
        const updated = taskList.value.find(t => t.taskId === selectedTask.value.taskId)
        if (updated) selectedTask.value = updated
      }
      if (selectedTask.value && selectedTask.value.status === 'RUNNING') {
        loadInlineLogs(selectedTask.value.taskId)
      }
    }
  }, 3000)
})

import {onUnmounted} from 'vue'

onUnmounted(() => {
  stopBatchProgressPolling()
})
</script>

<style lang="scss" scoped>
.training-management {
  position: relative;
  min-height: 100vh;
  padding: 24px;
  padding-bottom: 60px;
  overflow: visible;
  width: 100%;
  box-sizing: border-box;

  // 背景粒子效果
  .background-particles {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 0;

    .particle {
      position: absolute;
      background: rgba(59, 130, 246, 0.3);
      border-radius: 50%;
      animation: float-particle infinite ease-in-out;
    }
  }

  // 背景装饰圆形
  .background-circles {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 0;
    overflow: hidden;

    .circle {
      position: absolute;
      border-radius: 50%;
      animation: float-circle 20s ease-in-out infinite;

      &.circle-top {
        width: 600px;
        height: 600px;
        background: rgba(147, 197, 253, 0.2);
        top: -200px;
        right: -150px;
      }

      &.circle-bottom {
        width: 500px;
        height: 500px;
        background: rgba(191, 219, 254, 0.2);
        bottom: -150px;
        left: -100px;
        animation-duration: 25s;
        animation-direction: reverse;
      }

      &.circle-middle {
        width: 400px;
        height: 400px;
        background: rgba(224, 242, 254, 0.3);
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        animation-duration: 30s;
      }
    }
  }

  // 内容区域
  .content-wrapper {
    position: relative;
    z-index: 1;
  }

  // 头部Logo区域
  .header-section {
    text-align: center;
    margin-bottom: 30px;
    padding-top: 10px;

    .logo-container {
      margin-bottom: 10px;

      .logo-circle {
        width: 100px;
        height: 100px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.95);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
        border: 3px solid rgba(59, 130, 246, 0.2);
        transition: all 0.3s ease;

        &:hover {
          transform: scale(1.05);
          box-shadow: 0 15px 40px rgba(0, 0, 0, 0.2);
        }

        .el-icon {
          filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
        }
      }
    }

    .header-title {
      font-size: 36px;
      font-weight: 800;
      color: #1e293b;
      margin: 0 0 10px 0;
      text-align: center;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      letter-spacing: 1px;
    }

    .header-subtitle {
      font-size: 18px;
      color: #475569;
      font-weight: 500;
      margin: 0;
      text-align: center;
      letter-spacing: 0.5px;
    }
  }

  // 深色模式适配
  html.dark & {
    .header-title {
      color: #ffffff;
      text-shadow: 0 2px 8px rgba(0, 0, 0, 0.5), 0 0 20px rgba(255, 255, 255, 0.3);
    }

    .header-subtitle {
      color: #e2e8f0;
      text-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
    }
  }

  // 通用卡片样式
  .modern-card {
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(20px);
    border-radius: 28px;
    box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.5);
    transition: all 0.3s ease;
    overflow: visible;
    position: relative;
    z-index: 1;

    &:hover {
      transform: translateY(-6px);
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
      background: rgba(255, 255, 255, 0.98);
    }
  }

  .search-card {
    margin-bottom: 24px;
    padding: 8px 12px;
    overflow: visible;

    :deep(.el-card__body) {
      padding: 0;
    }

    :deep(.el-form) {
      display: flex;
      align-items: center;
      flex-wrap: nowrap;
      width: 100%;

      .el-form-item {
        margin-bottom: 0;
        margin-right: 20px;
        flex-shrink: 0;
        white-space: nowrap;

        &:first-child {
          margin-left: 20px;
        }
      }
    }
  }

  .stats-row {
    margin-bottom: 24px;

    .stat-card {
      position: relative;
      height: 130px;
      border-radius: 24px;
      overflow: hidden;
      cursor: pointer;
      transition: all 0.3s ease;
      box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.3);
      backdrop-filter: blur(10px);

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 16px 32px rgba(0, 0, 0, 0.15);

        .stat-background {
          transform: scale(1.1);
        }
      }

      .stat-background {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        transition: transform 0.3s ease;
      }

      .stat-content {
        position: relative;
        z-index: 2;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 16px;
        height: 100%;
        color: white;

        .stat-icon {
          font-size: 32px;
          margin-bottom: 0;
          filter: brightness(1.8);
        }

        .stat-label {
          font-size: 14px;
          opacity: 1;
          font-weight: 400;
          margin-bottom: 6px;
          margin-top: -6px;
          text-align: center;
        }

        .stat-value {
          font-size: 26px;
          font-weight: 700;
          line-height: 1;
          text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
        }
      }

      // 第2个和第4个卡片的图标更亮
      &.success .stat-content .stat-icon,
      &.danger .stat-content .stat-icon {
        filter: brightness(2.2);
      }

      // 不同主题色
      &.primary .stat-background {
        background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
      }

      &.success .stat-background {
        background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%);
      }

      &.warning .stat-background {
        background: linear-gradient(135deg, #E6A23C 0%, #ebb563 100%);
      }

      &.danger .stat-background {
        background: linear-gradient(135deg, #ff6b9d 0%, #ff8fab 100%);
      }
    }
  }
}

.el-form-item {
  .el-icon {
    color: #909399;

    &:hover {
      color: #409EFF;
    }
  }
}

.table-card {
  margin-bottom: 24px;
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

.terminal-log {
  background: #111;
  color: #0f0;
  padding: 12px;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 13px;
  white-space: pre-wrap;
  line-height: 1.5;
  min-height: 100%;
}

/* 实时进度条样式 */
.real-time-progress {
  background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%);
  border: 1px solid #00ff00;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 20px;
  font-family: 'Courier New', Courier, monospace;
  box-shadow: 0 0 20px rgba(0, 255, 0, 0.2);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.progress-epoch {
  color: #00ff00;
  font-size: 16px;
  font-weight: bold;
  text-shadow: 0 0 10px rgba(0, 255, 0, 0.5);
}

.progress-speed {
  color: #00cc88;
  font-size: 13px;
}

.progress-bar-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-counter {
  color: #00ff00;
  font-size: 14px;
  min-width: 100px;
  font-weight: bold;
}

.progress-bar-wrapper {
  flex: 1;
  position: relative;
}

.progress-bar-track {
  background: #1a1a1a;
  border: 1px solid #333;
  height: 24px;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.progress-bar-fill {
  background: linear-gradient(90deg, #00ff00 0%, #00cc00 100%);
  height: 100%;
  transition: width 0.3s ease-out;
  box-shadow: 0 0 10px rgba(0, 255, 0, 0.5);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 10px rgba(0, 255, 0, 0.5);
  }
  50% {
    box-shadow: 0 0 20px rgba(0, 255, 0, 0.8);
  }
}

.progress-bar-text {
  position: absolute;
  top: 50%;
  left: 8px;
  transform: translateY(-50%);
  color: #00ff00;
  font-size: 12px;
  letter-spacing: 0;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.8);
  pointer-events: none;
}

// 动画效果
@keyframes float-particle {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(20px, -20px);
  }
}

@keyframes float-circle {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(30px, -30px) scale(1.1);
  }
}
</style>