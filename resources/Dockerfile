FROM fnproject/fn-java-fdk
WORKDIR /function
CMD ["codes.recursive.imageanalysis.ImageAnalysis::analyze"]
LABEL build.id=64080a5d7af63e9a6c0a40d9dff361e0347e8d7f755c8e7f21a1f392404f32d2
ADD libs/* /function/app/
ADD func.yaml /function/app/
WORKDIR /
COPY .oci/ /.oci