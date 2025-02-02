package frc.robot.subsystems.drivetrain;

import com.kauailabs.navx.frc.AHRS;
import com.revrobotics.CANSparkMax;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.*;
import edu.wpi.first.wpilibj.RobotState;
import edu.wpi.first.wpilibj2.command.Subsystem;
import frc.robot.Constants;
import frc.robot.Robot;
import frc.robot.commands.drivetrain.DefaultDriveCommand;
import lib.MoreMath;
import org.littletonrobotics.junction.Logger;

public class DrivetrainSubsystem implements Subsystem {

	private static DrivetrainSubsystem drivetrain;

	private final SwerveModuleIO front_left;
	private final SwerveModuleIO front_right;
	private final SwerveModuleIO back_left;
	private final SwerveModuleIO back_right;

	private final AHRS gyro;

	private final SwerveDriveKinematics kinematics;

	private final SwerveModulePosition[] modulePositions = {null, null, null, null};
	private final SwerveDrivePoseEstimator poseEstimator;
	private final SwerveDriveOdometry odometry;
	private final PIDController rotateController = new PIDController(1,0,0);
	private ChassisSpeeds autoAline;

	private SwerveModuleState[] autoStates;
	private SwerveModuleState[] moduleStates = {new SwerveModuleState(), new SwerveModuleState(), new SwerveModuleState(), new SwerveModuleState()};
	private double rotateTarget;


	public static DrivetrainSubsystem getInstance()
	{
		if (drivetrain == null)
		{
			drivetrain = new DrivetrainSubsystem();
			drivetrain.setDefaultCommand(new DefaultDriveCommand(() -> Constants.driveController, drivetrain));

			return drivetrain;
		}else{
			return drivetrain;
		}
	}

	private DrivetrainSubsystem() {

		gyro = new AHRS();
		if (Robot.isSimulation())
		{
			front_left = new ModuleSim(
					Constants.Drivetrain.frontLeft.DRIVE_ID,
					Constants.Drivetrain.frontLeft.STEER_ID,
					Constants.Drivetrain.frontLeft.CAN_CODER_ID,
					Constants.Drivetrain.frontLeft.CAN_CODER_OFFSET,
					Modules.FL
			);
			front_right = new ModuleSim(
					Constants.Drivetrain.frontRight.DRIVE_ID,
					Constants.Drivetrain.frontRight.STEER_ID,
					Constants.Drivetrain.frontRight.CAN_CODER_ID,
					Constants.Drivetrain.frontRight.CAN_CODER_OFFSET,
					Modules.FR
			);
			back_left = new ModuleSim(
					Constants.Drivetrain.backLeft.DRIVE_ID,
					Constants.Drivetrain.backLeft.STEER_ID,
					Constants.Drivetrain.backLeft.CAN_CODER_ID,
					Constants.Drivetrain.backLeft.CAN_CODER_OFFSET,
					Modules.BL
			);
			back_right = new ModuleSim(
					Constants.Drivetrain.backRight.DRIVE_ID,
					Constants.Drivetrain.backRight.STEER_ID,
					Constants.Drivetrain.backRight.CAN_CODER_ID,
					Constants.Drivetrain.backRight.CAN_CODER_OFFSET,
					Modules.BR
			);
		} else { // This will happen if the robot is real
			front_left = new ModuleReal(
					Constants.Drivetrain.frontLeft.DRIVE_ID,
					Constants.Drivetrain.frontLeft.STEER_ID,
					Constants.Drivetrain.frontLeft.CAN_CODER_ID,
					Constants.Drivetrain.frontLeft.CAN_CODER_OFFSET,
					Modules.FL
			);
			front_right = new ModuleReal(
					Constants.Drivetrain.frontRight.DRIVE_ID,
					Constants.Drivetrain.frontRight.STEER_ID,
					Constants.Drivetrain.frontRight.CAN_CODER_ID,
					Constants.Drivetrain.frontRight.CAN_CODER_OFFSET,
					Modules.FR
			);
			back_left = new ModuleReal(
					Constants.Drivetrain.backLeft.DRIVE_ID,
					Constants.Drivetrain.backLeft.STEER_ID,
					Constants.Drivetrain.backLeft.CAN_CODER_ID,
					Constants.Drivetrain.backLeft.CAN_CODER_OFFSET,
					Modules.BL
			);
			back_right = new ModuleReal(
					Constants.Drivetrain.backRight.DRIVE_ID,
					Constants.Drivetrain.backRight.STEER_ID,
					Constants.Drivetrain.backRight.CAN_CODER_ID,
					Constants.Drivetrain.backRight.CAN_CODER_OFFSET,
					Modules.BR
			);
		}
		modulePositions[0] = front_left.getModulePosition();
		modulePositions[1] = front_right.getModulePosition();
		modulePositions[2] = back_left.getModulePosition();
		modulePositions[3] = back_right.getModulePosition();

		kinematics = Constants.Drivetrain.KINEMATICS;

		poseEstimator = new SwerveDrivePoseEstimator(kinematics, getGyroRotation(), modulePositions, new Pose2d());

		rotateController.enableContinuousInput(-180, 180);

		odometry = new SwerveDriveOdometry(kinematics, gyro.getRotation2d(), modulePositions, new Pose2d());

	}
	public void drive(double x, double y, double rotate)
	{
		x = x * Constants.Drivetrain.MAX_SPEED;
		y = y * Constants.Drivetrain.MAX_SPEED;
		rotate = rotate * Constants.Drivetrain.MAX_SPEED;

		SwerveModuleState[] states;

		if (Constants.Drivetrain.DRIVE_MODE == Constants.Drivetrain.MANUEL_DRIVE_MODE)
		{
			ChassisSpeeds speeds = new ChassisSpeeds(x, y, rotate);
			states = kinematics.toSwerveModuleStates(ChassisSpeeds.fromFieldRelativeSpeeds(speeds,getGyroRotation()));

		} else if (Constants.Drivetrain.DRIVE_MODE == Constants.Drivetrain.AUTO_TURN) {
			double rotateSpeed;
			rotateSpeed = rotateController.calculate(getGyroRotation().getDegrees(), rotateTarget - 180);
			rotateSpeed = rotateSpeed * Constants.Drivetrain.MAX_SPEED;
			rotateSpeed = MoreMath.minMax(rotateSpeed, 3,-3);

			ChassisSpeeds speeds = new ChassisSpeeds(x, y, rotateSpeed);

			states = kinematics.toSwerveModuleStates(ChassisSpeeds.fromFieldRelativeSpeeds(speeds, getGyroRotation()));
		} else if (Constants.Drivetrain.DRIVE_MODE == Constants.Drivetrain.AUTO_ALINE) {
			states = kinematics.toSwerveModuleStates(ChassisSpeeds.fromFieldRelativeSpeeds(autoAline, getGyroRotation()));
		}else if(Constants.Drivetrain.DRIVE_MODE == Constants.Drivetrain.AUTO_DRIVE_MODE){

			states = autoStates;
		}else{
			states = kinematics.toSwerveModuleStates(new ChassisSpeeds(0,0,0));
		}

		if (Robot.isSimulation()) {
			double roationAmount = kinematics.toChassisSpeeds(states).omegaRadiansPerSecond;
			if (roationAmount >= .1 || roationAmount <= -.1)
			{
				gyro.setAngleAdjustment(gyro.getAngleAdjustment() + (roationAmount * Constants.Drivetrain.TURNING_SPEED_SIM));
			}
		}

		moduleStates = states;

		setModuleStates(states);
	}



	private void setModuleStates(SwerveModuleState[] moduleStates)
	{
		front_left.setModule(
				moduleStates[0].speedMetersPerSecond,
				moduleStates[0].angle.getDegrees()
		);
		front_right.setModule(
				moduleStates[1].speedMetersPerSecond,
				moduleStates[1].angle.getDegrees()
		);
		back_left.setModule(
				moduleStates[2].speedMetersPerSecond,
				moduleStates[2].angle.getDegrees()
		);
		back_right.setModule(
				moduleStates[3].speedMetersPerSecond,
				moduleStates[3].angle.getDegrees()
		);
	}

	public void setBreak()
	{
		front_left.setMode(CANSparkMax.IdleMode.kBrake);
		front_right.setMode(CANSparkMax.IdleMode.kBrake);
		back_left.setMode(CANSparkMax.IdleMode.kBrake);
		back_right.setMode(CANSparkMax.IdleMode.kBrake);
	}
	public void setCoast()
	{
		front_left.setMode(CANSparkMax.IdleMode.kCoast);
		front_right.setMode(CANSparkMax.IdleMode.kCoast);
		back_left.setMode(CANSparkMax.IdleMode.kCoast);
		back_right.setMode(CANSparkMax.IdleMode.kCoast);
	}
	private void teleopPeriodic()
	{

	}
	private void autoPeriodic()
	{

	}
	@Override
	public void periodic() {

		front_left.update();
		front_right.update();
		back_right.update();
		back_left.update();

		if (Constants.Drivetrain.DRIVE_MODE == Constants.Drivetrain.AUTO_DRIVE_MODE)
		{
			if (autoStates == null)
			{
				setAutoStates(getKinematics().toSwerveModuleStates(new ChassisSpeeds(0,0,0)));
			}
			drive(0,0,0);
		}

		if (RobotState.isTeleop())
		{
			teleopPeriodic();
		}
		if (RobotState.isAutonomous())
		{
			autoPeriodic();
		}
		if (RobotState.isEnabled())
		{
			modulePositions[0] = front_left.getModulePosition();
			modulePositions[1] = front_right.getModulePosition();
			modulePositions[2] = back_left.getModulePosition();
			modulePositions[3] = back_right.getModulePosition();

			poseEstimator.update(getGyroRotation(), modulePositions);
		}
		double[] states =
				{
						modulePositions[0].angle.getDegrees(), modulePositions[0].distanceMeters,
						modulePositions[1].angle.getDegrees(), modulePositions[1].distanceMeters,
						modulePositions[2].angle.getDegrees(), modulePositions[2].distanceMeters,
						modulePositions[3].angle.getDegrees(), modulePositions[3].distanceMeters
				};

		if (Robot.isSimulation())
		{
			modulePositions[0] = front_left.getModulePosition();
			modulePositions[1] = front_right.getModulePosition();
			modulePositions[2] = back_left.getModulePosition();
			modulePositions[3] = back_right.getModulePosition();

			odometry.update(gyro.getRotation2d(), modulePositions);
		}

		double targetStates[] =
				{
						front_left.getTargetStates().angle.getDegrees(),front_left.getTargetStates().speedMetersPerSecond,
						front_right.getTargetStates().angle.getDegrees(),front_right.getTargetStates().speedMetersPerSecond,
						back_left.getTargetStates().angle.getDegrees(),	back_left.getTargetStates().speedMetersPerSecond,
						back_right.getTargetStates().angle.getDegrees(),back_right.getTargetStates().speedMetersPerSecond
				};
		double currentState[] =
				{
						front_left.getCurrentPosition().angle.getDegrees(),front_left.getCurrentPosition().speedMetersPerSecond,
						front_right.getCurrentPosition().angle.getDegrees(),front_right.getCurrentPosition().speedMetersPerSecond,
						back_left.getCurrentPosition().angle.getDegrees(),back_left.getCurrentPosition().speedMetersPerSecond,
						back_right.getCurrentPosition().angle.getDegrees(),back_right.getCurrentPosition().speedMetersPerSecond,
				};
		Logger.getInstance().recordOutput("Drivetrain/CurrentModuleState", currentState);
		Logger.getInstance().recordOutput("Drivetrain/TargetStatesModule", targetStates);
		Logger.getInstance().recordOutput("Drivetrain/ModuleStates", moduleStates);
		Logger.getInstance().recordOutput("Drivetrain/Gyro", gyro.getRotation2d().getRadians());
		Logger.getInstance().recordOutput("Drivetrain/Position", odometry.getPoseMeters());
	}

	public Rotation2d getGyroRotation()
	{
		return gyro.getRotation2d();
	}

	public void resetGyro()
	{
		gyro.zeroYaw();
	}
	public void setRotateTarget(double mRotateTarget) {
		rotateTarget = mRotateTarget;
	}
	public boolean atSetpoint(){return rotateController.atSetpoint();}
	public void setAutoAline(ChassisSpeeds mAutoAline) {
		autoAline = mAutoAline;
	}

	public Pose2d getPose()
	{
		return odometry.getPoseMeters();
	}
	public void resetPos(Pose2d pos)
	{
		odometry.resetPosition(getGyroRotation(),modulePositions, pos);
	}
	public void setAutoStates(SwerveModuleState[] mAutoStates) {
		autoStates = mAutoStates;
	}
	public SwerveDriveKinematics getKinematics() {
		return kinematics;
	}
}

