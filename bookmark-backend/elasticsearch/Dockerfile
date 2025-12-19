FROM docker.elastic.co/elasticsearch/elasticsearch:8.10.0

# 针对 Mac arm64 架构安装 IK 分词器
RUN ./bin/elasticsearch-plugin install --batch https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.10.0.zip